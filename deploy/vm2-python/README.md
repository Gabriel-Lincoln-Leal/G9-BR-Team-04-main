# VM 2 — Python e modelos

Nova VM Ubuntu 24.04 com 1 GB, na mesma VCN da VM 1. Instalar Python 3.12, suporte a venv e configurar swap de 2 GB. Não precisa de Java, Nginx ou Node.js.

## Arquivos a instalar

Copiar de `api-python/` somente `app/`, `models/` e `requirements.txt` para `/opt/techmind/python/`. Não copiar o venv Windows.

Criar usuário de serviço `techmind`, venv Linux em `/opt/techmind/python/.venv` e instalar o requirements nesse ambiente. Usar um worker e uma thread por biblioteca numérica.

## Configuração

Arquivo `/etc/techmind/techmind.env`, pertencente a root, modo 600; diretório modo 700:

```ini
TECHMIND_API_KEY=VALOR_REAL_DA_CHAVE_EXISTENTE_NA_VM1
OPENBLAS_NUM_THREADS=1
OMP_NUM_THREADS=1
MKL_NUM_THREADS=1
```

Não versionar valores reais. A VM 2 não recebe credenciais Oracle.

Unidade systemd: `/etc/systemd/system/techmind-python.service`, com usuário `techmind`, WorkingDirectory `/opt/techmind/python`, EnvironmentFile acima e Restart=on-failure.

Comando ExecStart, substituindo o marcador pelo IP privado real da VM 2:

```text
/opt/techmind/python/.venv/bin/python -m uvicorn app.main:app --host IP_PRIVADO_VM2 --port 8000 --workers 1 --limit-concurrency 4
```

## Rede e validação

- Entrada TCP 8000 somente do IP privado da VM 1 `/32`, tanto na OCI quanto no firewall Linux.
- SSH apenas das origens administrativas.
- Saída DNS/HTTPS para instalação de dependências.
- IP público pode ser usado para administração e saída à internet; Python escuta apenas no IP privado.

Da VM 1, executar `curl --fail http://IP_PRIVADO_VM2:8000/health` e verificar o indicador do modelo. Testar também uma classificação autenticada antes da migração. O health check sozinho não verifica a chave compartilhada.

Não abrir TCP 8000 para `0.0.0.0/0`. Conferir regras existentes da subnet, pois uma regra restrita adicional não elimina uma permissão ampla já existente.
