# Executar o TechMind localmente

Use Java 21, Python 3.12, Node.js 24, npm e Docker. Execute os comandos abaixo em terminais PowerShell separados, a partir desta pasta. A primeira instalação das dependências exige acesso à internet.

## 1. Oracle Database

Para desenvolvimento local, escolha uma senha e inicie o banco. Aguarde `DATABASE IS READY TO USE!` no log.

```powershell
$env:DB_PASSWORD = 'uma-senha-local-forte'
docker volume create techmind-oradata
docker run -d --name techmind-oracle -p 1521:1521 -e ORACLE_PWD=$env:DB_PASSWORD -v techmind-oradata:/opt/oracle/oradata container-registry.oracle.com/database/free:latest-lite
docker logs -f techmind-oracle
```

O exemplo usa `SYSTEM` apenas para começar localmente. Para produção, crie um usuário dedicado com privilégios mínimos e migrações de esquema.

## 2. Chave de acesso

Gere uma chave aleatória. Use a **mesma** chave no backend e no Python. Não a salve no repositório.

```powershell
$env:TECHMIND_API_KEY = [Convert]::ToHexString([Security.Cryptography.RandomNumberGenerator]::GetBytes(32))
$env:TECHMIND_API_KEY
```

Copie o valor para os outros terminais. O frontend solicitará essa chave no primeiro acesso. Ela fica em `sessionStorage` e é removida quando a aba é encerrada. Em produção, sirva a aplicação somente por HTTPS.

## 3. Serviço de classificação

```powershell
cd api-python
py -3.12 -m venv .venv
.\.venv\Scripts\Activate.ps1
python -m pip install -r requirements.txt
$env:TECHMIND_API_KEY = 'valor-gerado-na-etapa-2'
python -m uvicorn app.main:app --host 127.0.0.1 --port 8000
```

Confira `http://127.0.0.1:8000/health`: `modelo_carregado` deve ser `true`. Os artefatos locais em `models/` são suficientes. Para atualizar por URL, configure cada `TECHMIND_*_URL` junto de seu `TECHMIND_*_SHA256`; somente HTTPS é aceito. Arquivos pickle devem vir de fonte confiável.

## 4. Backend

```powershell
cd backend
$env:DB_USERNAME = 'SYSTEM'
$env:DB_PASSWORD = 'a-mesma-senha-da-etapa-1'
$env:DB_DDL_AUTO = 'update'
$env:TECHMIND_API_KEY = 'valor-gerado-na-etapa-2'
.\mvnw.cmd spring-boot:run
```

O backend usa `localhost:1521/FREEPDB1` e `127.0.0.1:8000` por padrão. `DB_DDL_AUTO=update` é somente para desenvolvimento; em produção use migrações e `validate`.

Teste a API:

```powershell
$headers = @{ Authorization = "Bearer $env:TECHMIND_API_KEY" }
Invoke-RestMethod -Uri 'http://localhost:8080/conteudo/titulo?titulo=' -Headers $headers
```

## 5. Frontend

```powershell
cd frontend
npm ci
npm start
```

Abra `http://localhost:4200` e informe a chave da etapa 2. O proxy de desenvolvimento encaminha `/conteudo` ao backend.

## Planejamento para OCI com 1 GB

Não instale Oracle Database na instância de 1 GB. Use um banco externo. Compile o Angular fora da VM e publique apenas seus arquivos estáticos em um servidor web leve. Mantenha backend e Python acessíveis apenas por loopback, com uma instância Python, e monitore memória antes de aceitar carga. Configure `DB_URL`, `DB_USERNAME`, `DB_PASSWORD`, `TECHMIND_API_KEY` e `DB_DDL_AUTO=validate` no ambiente. A conexão com Autonomous Database pode exigir wallet ou configuração TLS específica; o endereço JDBC local não serve na OCI.

## Segurança antes de publicar

A chave de acesso é compartilhada por quem usa a aplicação e concede acesso às rotas de conteúdo e usuários. Distribua-a apenas a pessoas autorizadas e troque-a se houver suspeita de vazamento. O backend escuta somente em `127.0.0.1` por padrão; publique o frontend por HTTPS com um proxy reverso e mantenha a API Python privada. O cadastro de usuários ainda não oferece login individual.

Instalações antigas podem conter senhas de usuários em texto puro. Os novos cadastros e alterações usam PBKDF2 com salt, mas as senhas antigas exigem redefinição; não há migração automática segura. Antes de publicar, crie um usuário próprio no Oracle, defina as tabelas com migrações e teste o consumo de memória na VM escolhida.
