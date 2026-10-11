from contextlib import asynccontextmanager
import logging
import os
from secrets import compare_digest

from fastapi import Depends, FastAPI, Header, HTTPException
from fastapi.responses import JSONResponse

from app.model_service import ModelNotLoadedError, classification_service
from app.schemas import MlPredicaoRequest, MlPredicaoResponse

logger = logging.getLogger(__name__)
API_KEY = os.getenv("TECHMIND_API_KEY", "")
if len(API_KEY) < 32:
    raise RuntimeError("TECHMIND_API_KEY deve ter pelo menos 32 caracteres")


def require_api_key(authorization: str | None = Header(default=None)) -> None:
    if not authorization or not authorization.startswith("Bearer ") or not compare_digest(
        authorization[7:], API_KEY
    ):
        raise HTTPException(status_code=401, detail="Não autorizado")


@asynccontextmanager
async def lifespan(app: FastAPI):
    try:
        classification_service.load()
        logger.info("Modelo carregado com sucesso")
    except ModelNotLoadedError as e:
        logger.error("Modelo indisponível: %s", e)
    yield


app = FastAPI(
    title="TechMind - Serviço de Classificação (Ciência de Dados)",
    description="Serviço interno consumido pela API principal (Back-End Java) para classificar conteúdos técnicos.",
    version="1.0.0",
    lifespan=lifespan,
)

@app.get("/health")
def health_check():
    return {
        "status": "ok",
        "modelo_carregado": classification_service.is_loaded,
    }


@app.post("/predizer", response_model=MlPredicaoResponse, dependencies=[Depends(require_api_key)])
def predizer(payload: MlPredicaoRequest):
    try:
        # Passando título e texto para o serviço
        resultado = classification_service.classificar(
            titulo=payload.titulo,
            texto=payload.texto
        )
    except ModelNotLoadedError:
        raise HTTPException(status_code=503, detail="Modelo indisponível")

    return resultado


@app.exception_handler(Exception)
async def unhandled_exception_handler(request, exc):
    logger.exception("Erro inesperado na classificação", exc_info=exc)
    return JSONResponse(
        status_code=500,
        content={"detail": "Erro interno ao processar a requisição"},
    )
