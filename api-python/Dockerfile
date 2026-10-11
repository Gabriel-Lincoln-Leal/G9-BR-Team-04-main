# Usa uma imagem oficial do Python otimizada e leve
FROM python:3.12-slim

# Define o diretório de trabalho dentro do contêiner
WORKDIR /app

# Copia e instala as dependências do Python primeiro (otimiza o cache do Docker)
COPY requirements.txt .
RUN pip install --no-cache-dir -r requirements.txt

# Copia o código da aplicação e os modelos treinados
COPY ./app ./app
# Se a pasta models estiver na raiz, copia ela também para garantir que o modelo seja encontrado
COPY ./models ./models
RUN groupadd --system techmind && useradd --system --gid techmind techmind \
    && chown -R techmind:techmind /app
USER techmind

# Porta interna do serviço
EXPOSE 8000

# Uma única instância do Uvicorn para economizar memória
CMD ["sh", "-c", "uvicorn app.main:app --host 0.0.0.0 --port ${PORT:-8000}"]
