"""
Carrega os artefatos treinados (vectorizer.pkl, modelo.pkl, tfidf_keywords.pkl)
e expõe a função de classificação.

Os arquivos são baixados do OCI Object Storage na primeira inicialização e
ficam em cache em models/. Se o download falhar, usa a cópia local existente,
se houver.
"""

import os
import pickle
import hashlib
import logging
from pathlib import Path

import numpy as np
import requests

MODELS_DIR = Path(__file__).resolve().parent.parent / "models"
VECTORIZER_PATH = MODELS_DIR / "vectorizer.pkl"
VECTORIZER_KEYWORDS_PATH = MODELS_DIR / "tfidf_keywords.pkl"
MODEL_PATH = MODELS_DIR / "modelo.pkl"

MODEL_URLS = {
    MODEL_PATH: (os.getenv("TECHMIND_MODELO_URL", ""), os.getenv("TECHMIND_MODELO_SHA256", "")),
    VECTORIZER_KEYWORDS_PATH: (os.getenv("TECHMIND_TFIDF_KEYWORDS_URL", ""), os.getenv("TECHMIND_TFIDF_KEYWORDS_SHA256", "")),
    VECTORIZER_PATH: (os.getenv("TECHMIND_VECTORIZER_URL", ""), os.getenv("TECHMIND_VECTORIZER_SHA256", "")),
}

DOWNLOAD_TIMEOUT_SECONDS = 30
MAX_MODEL_BYTES = 20 * 1024 * 1024
TOP_N_KEYWORDS = 5
logger = logging.getLogger(__name__)


class ModelNotLoadedError(RuntimeError):
    """Lançado quando os artefatos do modelo não foram encontrados/carregados."""


def _baixar_artefato(destino: Path, url: str, expected_sha256: str) -> bool:
    """
    Plano A: baixa sempre do OCI Object Storage, sobrescrevendo a cópia local
    (mantém o cache atualizado). Plano B: se o download falhar, usa a cópia
    local existente, se houver.
    """
    if url:
        if not url.startswith("https://") or len(expected_sha256) != 64 or any(
            c not in "0123456789abcdefABCDEF" for c in expected_sha256
        ):
            raise ModelNotLoadedError("URL HTTPS e SHA-256 válido são obrigatórios para modelos remotos")
        temp_path = destino.with_suffix(destino.suffix + ".tmp")
        try:
            MODELS_DIR.mkdir(parents=True, exist_ok=True)
            digest = hashlib.sha256()
            size = 0
            with requests.get(url, timeout=DOWNLOAD_TIMEOUT_SECONDS, stream=True) as resposta:
                resposta.raise_for_status()
                with temp_path.open("wb") as output:
                    for chunk in resposta.iter_content(chunk_size=65536):
                        size += len(chunk)
                        if size > MAX_MODEL_BYTES:
                            raise ModelNotLoadedError("Artefato remoto excede 20 MB")
                        digest.update(chunk)
                        output.write(chunk)
            if digest.hexdigest().lower() != expected_sha256.lower():
                raise ModelNotLoadedError("SHA-256 do artefato remoto não confere")
            temp_path.replace(destino)
            logger.info("Artefato %s atualizado (%d bytes)", destino.name, size)
            return True
        except (requests.RequestException, OSError, ModelNotLoadedError):
            logger.warning("Falha ao atualizar %s; usando cópia local, se disponível", destino.name)
        finally:
            temp_path.unlink(missing_ok=True)

    if destino.exists():
        logger.info("Usando cópia local de %s", destino.name)
        return True

    return False


class ClassificationService:
    def __init__(self) -> None:
        self._vectorizer = None
        self._model = None
        self._vectorizer_keywords = None

    def load(self) -> None:
        for destino, (url, expected_sha256) in MODEL_URLS.items():
            _baixar_artefato(destino, url, expected_sha256)

        if not VECTORIZER_PATH.exists() or not MODEL_PATH.exists():
            raise ModelNotLoadedError(
                f"Arquivos do modelo não encontrados em {MODELS_DIR} nem no Object Storage. "
                "Copie vectorizer.pkl, modelo.pkl e tfidf_keywords.pkl para essa pasta manualmente, "
                "ou confirme as URLs em TECHMIND_MODELO_URL / TECHMIND_VECTORIZER_URL / "
                "TECHMIND_TFIDF_KEYWORDS_URL (veja o README)."
            )

        with open(VECTORIZER_PATH, "rb") as f:
            self._vectorizer = pickle.load(f)

        with open(MODEL_PATH, "rb") as f:
            self._model = pickle.load(f)

        if VECTORIZER_KEYWORDS_PATH.exists():
            with open(VECTORIZER_KEYWORDS_PATH, "rb") as f:
                self._vectorizer_keywords = pickle.load(f)
        else:
            self._vectorizer_keywords = self._vectorizer

    @property
    def is_loaded(self) -> bool:
        return (
            self._vectorizer is not None
            and self._model is not None
            and self._vectorizer_keywords is not None
        )

    def _extrair_palavras_chave(self, texto: str, top_n: int = TOP_N_KEYWORDS) -> list[str]:
        vetor = self._vectorizer_keywords.transform([texto])
        vetor_denso = vetor.toarray()[0]

        if vetor_denso.sum() == 0:
            return []

        indices_top = vetor_denso.argsort()[::-1][:top_n]
        indices_top = [i for i in indices_top if vetor_denso[i] > 0]

        vocab_array = np.array(self._vectorizer_keywords.get_feature_names_out())
        return [vocab_array[i] for i in indices_top]

    def classificar(self, titulo: str, texto: str) -> dict:
        if not self.is_loaded:
            raise ModelNotLoadedError("Modelo ainda não foi carregado.")

        texto_completo = f"{titulo} {texto}"

        probabilidades = self._model.predict_proba([texto_completo])[0]
        classes = self._model.classes_

        indice_predito = int(np.argmax(probabilidades))
        categoria = str(classes[indice_predito])
        confianca = float(probabilidades[indice_predito])

        palavras_chave = self._extrair_palavras_chave(texto_completo)

        return {
            "categoria": categoria,
            "confianca": round(confianca, 4),
            "palavras_chave": palavras_chave,
        }


classification_service = ClassificationService()
