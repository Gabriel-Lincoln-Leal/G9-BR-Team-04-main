# TechMind na OCI Always Free: VM AMD de 1 GB

Este roteiro usa Ubuntu 24.04 LTS x86_64, Java 21, Python 3.12 e Nginx na VM; o Oracle fica no Autonomous Database Always Free. Compile Java e Angular no computador local. Não é necessário Docker. A capacidade de 1 GB ainda precisa ser validada com a aplicação em execução; swap evita algumas falhas por falta de memória, mas pode deixar as requisições lentas.

Arquitetura: navegador → HTTPS/Nginx → backend em 127.0.0.1:8080 → Python em 127.0.0.1:8000; backend → Autonomous Database por TLS. O Angular é servido como arquivos estáticos.

## 1. Criar a VM e configurar a rede

No console OCI, na região principal da conta, abra Compute → Instances → Create instance. Escolha Ubuntu 24.04 LTS e a shape VM.Standard.E2.1.Micro (AMD, 1 GB). Confirme a indicação Always Free eligible. Mantenha um boot volume de 50 GB, respeitando o total gratuito de volumes da conta. Crie ou selecione uma VCN com subnet pública, Internet Gateway e rota 0.0.0.0/0 para esse gateway. Habilite IPv4 público. Salve a chave SSH privada e anote o IP público.

Na Security List ou NSG associado à VM, configure regras TCP de entrada com portas de origem livres: destino 22 somente do seu IP público/32; destinos 80 e 443 de 0.0.0.0/0 quando publicar o site. Mantenha a saída necessária para HTTPS, DNS e a porta indicada pela conexão TLS do banco. Não abra 8000, 8080 ou 1521 como portas de entrada.

No PowerShell do Windows, substitua o caminho da chave e IP_VM:

```powershell
ssh -i "C:\caminho\chave-oci.key" ubuntu@IP_VM
```

Os comandos Linux das próximas etapas são executados nessa conexão SSH.

## 2. Criar o banco gratuito

No console OCI, abra Autonomous Database → Create. Escolha a opção Always Free, workload Transaction Processing e crie a senha de ADMIN. Confirme que o recurso é gratuito antes de criar.

Em Network/Access control, permita conexões do IP público da VM como um endereço /32. Habilite conexões TLS sem exigir apenas mTLS. Os nomes das opções variam conforme a versão do console. Em Database connection, selecione TLS e copie a connection string do serviço terminado em _low. O endereço JDBC será `jdbc:oracle:thin:@` seguido da descrição completa copiada, incluindo `ssl_server_dn_match=yes`. Não use a URL local FREEPDB1. Não é necessário wallet quando a conexão TLS sem wallet está habilitada.

Abra Database Actions → SQL, conectado como ADMIN, e execute, trocando a senha:

```sql
CREATE USER TECHMIND IDENTIFIED BY "TroquePorUmaSenhaForte123";
GRANT CREATE SESSION, CREATE TABLE, CREATE SEQUENCE TO TECHMIND;
ALTER USER TECHMIND QUOTA 100M ON DATA;
```

Esse usuário próprio será usado pelo backend. A primeira inicialização criará o esquema vazio; depois a configuração passará para validate. Isso é um procedimento inicial para demonstração. Para futuras alterações em dados existentes, use migrações revisadas e backup.

## 3. Preparar a VM

```bash
sudo apt update
sudo apt install -y openjdk-21-jre-headless python3.12-venv python3-pip nginx unzip openssl
java -version
python3.12 --version
sudo adduser --system --group --home /opt/techmind techmind
sudo install -d -o techmind -g techmind /opt/techmind/python /opt/techmind/logs
sudo install -d -m 755 /var/www/techmind
```

Confira a swap existente:

```bash
swapon --show
```

Se não houver swap e /swapfile não existir, crie 2 GB:

```bash
sudo fallocate -l 2G /swapfile
sudo chmod 600 /swapfile
sudo mkswap /swapfile
sudo swapon /swapfile
echo '/swapfile none swap sw 0 0' | sudo tee -a /etc/fstab
free -h
```

Execute essa criação somente uma vez. Não recrie uma swap já ativa.

## 4. Compilar no Windows e enviar os arquivos

No computador local, use JDK 21 e Node.js 24. Abra PowerShell na pasta que contém backend, frontend e api-python:

```powershell
Set-Location 'C:\Users\admin\OneDrive\Desktop\Github TechMind OCI\G9-BR-Team-04-main\G9-BR-Team-04-main'
Push-Location backend
.\mvnw.cmd clean package
Pop-Location
Push-Location frontend
npm ci
npm run build-prod
Pop-Location
```

Se algum comando falhar, resolva o erro antes de enviar. O JAR esperado é backend/target/techmind-0.0.1-SNAPSHOT.jar. O Angular gera frontend/dist/techmind-frontend/browser; confirme que essa pasta contém index.html.

Ainda no mesmo PowerShell, substitua a chave e IP_VM:

```powershell
scp -i "C:\caminho\chave-oci.key" .\backend\target\techmind-0.0.1-SNAPSHOT.jar ubuntu@IP_VM:/home/ubuntu/techmind.jar
scp -i "C:\caminho\chave-oci.key" -r .\api-python\app .\api-python\models .\api-python\requirements.txt ubuntu@IP_VM:/home/ubuntu/
scp -i "C:\caminho\chave-oci.key" -r .\frontend\dist\techmind-frontend\browser ubuntu@IP_VM:/home/ubuntu/
```

Na VM:

```bash
sudo install -o techmind -g techmind -m 640 /home/ubuntu/techmind.jar /opt/techmind/techmind.jar
sudo cp -r /home/ubuntu/app /home/ubuntu/models /home/ubuntu/requirements.txt /opt/techmind/python/
sudo chown -R techmind:techmind /opt/techmind/python
sudo cp -r /home/ubuntu/browser/. /var/www/techmind/
sudo find /var/www/techmind -type d -exec chmod 755 {} \;
sudo find /var/www/techmind -type f -exec chmod 644 {} \;
sudo -u techmind python3.12 -m venv /opt/techmind/python/.venv
sudo -u techmind /opt/techmind/python/.venv/bin/python -m pip install --no-cache-dir --only-binary=:all: -r /opt/techmind/python/requirements.txt
```

Os modelos locais já existem no projeto; não precisa configurar Object Storage. Se o pip não encontrar um pacote binário compatível, confira a versão do Python e o erro antes de tentar compilar dependências na VM de 1 GB.

## 5. Configurar os segredos e a conexão

Gere uma chave de acesso na VM:

```bash
openssl rand -hex 32
sudo install -d -m 700 /etc/techmind
sudo install -m 600 /dev/null /etc/techmind/techmind.env
sudo nano /etc/techmind/techmind.env
```

Cole o seguinte, substituindo chave, senha e conexão. Não coloque `export`. Mantenha DB_URL inteiro em uma única linha, entre aspas simples:

```ini
TECHMIND_API_KEY=COLE_A_CHAVE_GERADA
DB_USERNAME=TECHMIND
DB_PASSWORD='SENHA_DO_USUARIO_TECHMIND'
DB_URL='jdbc:oracle:thin:@COLE_A_DESCRICAO_TLS_COMPLETA'
DB_DDL_AUTO=update
SERVER_ADDRESS=127.0.0.1
CLASSIFIER_BASE_URL=http://127.0.0.1:8000
SPRING_DATASOURCE_HIKARI_MAXIMUM_POOL_SIZE=2
SPRING_DATASOURCE_HIKARI_MINIMUM_IDLE=1
RESILIENCE4J_BULKHEAD_INSTANCES_CLASSIFICADOR_MAX_CONCURRENT_CALLS=1
OPENBLAS_NUM_THREADS=1
OMP_NUM_THREADS=1
MKL_NUM_THREADS=1
```

Salve com Ctrl+O, Enter, e saia com Ctrl+X. A chave é compartilhada pelos serviços e pelas pessoas autorizadas a usar o frontend. Ela não oferece login individual.

## 6. Criar os serviços para iniciarem automaticamente

Crie o serviço Python:

```bash
sudo nano /etc/systemd/system/techmind-python.service
```

Conteúdo:

```ini
[Unit]
Description=TechMind classificador Python
Wants=network-online.target
After=network-online.target

[Service]
User=techmind
Group=techmind
WorkingDirectory=/opt/techmind/python
EnvironmentFile=/etc/techmind/techmind.env
ExecStart=/opt/techmind/python/.venv/bin/python -m uvicorn app.main:app --host 127.0.0.1 --port 8000 --workers 1 --limit-concurrency 4
Restart=on-failure
RestartSec=10
NoNewPrivileges=true
PrivateTmp=true
ProtectHome=true
ProtectSystem=full

[Install]
WantedBy=multi-user.target
```

Crie o serviço Java:

```bash
sudo nano /etc/systemd/system/techmind-backend.service
```

Conteúdo:

```ini
[Unit]
Description=TechMind backend Java
Wants=network-online.target techmind-python.service
After=network-online.target techmind-python.service

[Service]
User=techmind
Group=techmind
WorkingDirectory=/opt/techmind
EnvironmentFile=/etc/techmind/techmind.env
ExecStart=/usr/bin/java -Xms64m -Xmx256m -XX:+UseSerialGC -XX:MaxMetaspaceSize=160m -XX:ReservedCodeCacheSize=64m -XX:MaxDirectMemorySize=32m -XX:+ExitOnOutOfMemoryError -jar /opt/techmind/techmind.jar
Restart=on-failure
RestartSec=15
NoNewPrivileges=true
PrivateTmp=true
ProtectHome=true
ProtectSystem=full

[Install]
WantedBy=multi-user.target
```

Esses valores limitam componentes da JVM, mas não representam o consumo total do processo. Ajuste a partir das medições; não há garantia de caber em 1 GB.

Inicie e confira:

```bash
sudo systemctl daemon-reload
sudo systemctl enable --now techmind-python
curl -f http://127.0.0.1:8000/health
sudo systemctl enable --now techmind-backend
sudo journalctl -u techmind-backend -n 80 --no-pager
curl -f http://127.0.0.1:8080/actuator/health
```

Se o backend ainda estiver iniciando, aguarde e repita o curl. Só continue quando ambos responderem e o Python informar modelo_carregado=true. Depois que o backend criar as tabelas no banco novo, abra `/etc/techmind/techmind.env`, troque `DB_DDL_AUTO=update` por `DB_DDL_AUTO=validate` e execute:

```bash
sudo systemctl restart techmind-backend
```

Não deixe update habilitado para posteriores implantações com dados existentes.

## 7. Configurar o Nginx

```bash
sudo nano /etc/nginx/sites-available/techmind
```

Cole o conteúdo. Se já possuir domínio, troque `_` pelo domínio real:

```nginx
server {
    listen 80 default_server;
    server_name _;
    root /var/www/techmind;
    index index.html;
    client_max_body_size 2m;

    location ~ ^/(conteudo|users)(/|$) {
        proxy_pass http://127.0.0.1:8080;
        proxy_set_header Host $host;
        proxy_set_header X-Real-IP $remote_addr;
        proxy_set_header X-Forwarded-For $proxy_add_x_forwarded_for;
        proxy_set_header X-Forwarded-Proto $scheme;
        proxy_read_timeout 120s;
    }

    location / {
        try_files $uri $uri/ /index.html;
    }
}
```

Ative a configuração:

```bash
sudo unlink /etc/nginx/sites-enabled/default
sudo ln -s /etc/nginx/sites-available/techmind /etc/nginx/sites-enabled/techmind
sudo nginx -t
sudo systemctl enable nginx
sudo systemctl reload nginx
```

Execute unlink e ln somente na primeira configuração, se esses caminhos estiverem no estado padrão.

## 8. Testar pelo túnel SSH antes de publicar

Em um novo PowerShell local:

```powershell
ssh -i "C:\caminho\chave-oci.key" -L 8888:127.0.0.1:80 ubuntu@IP_VM
```

Deixe esse terminal aberto e acesse http://localhost:8888 no navegador. Informe a chave de acesso quando solicitada, classifique um conteúdo e confira se aparece na listagem. O túnel protege a conexão com a VM durante esse teste e não exige abrir 80/443 para a internet.

## 9. Publicar com domínio e HTTPS

Crie um registro DNS A apontando seu domínio para o IP público da VM. Use um IP reservado, se disponível na sua configuração, ou atualize o DNS e a ACL do banco quando o IP mudar. Configure `server_name` no Nginx com esse domínio e recarregue depois de `sudo nginx -t`.

Libere 80/443 no NSG ou Security List da OCI. Na imagem Ubuntu padrão da OCI, também confira o firewall do sistema. Para liberar apenas essas portas e persistir as regras iptables:

```bash
sudo apt install -y iptables-persistent
sudo iptables -I INPUT 1 -p tcp -m multiport --dports 80,443 -j ACCEPT
sudo netfilter-persistent save
```

Não limpe as regras existentes; elas incluem proteções e acesso SSH. Se você já configurou outro firewall, adapte a liberação a ele.

Instale o Certbot e obtenha o certificado, substituindo seu domínio:

```bash
sudo apt install -y snapd
sudo snap install --classic certbot
sudo /snap/bin/certbot --nginx -d seu-dominio.com --redirect
sudo /snap/bin/certbot renew --dry-run
```

O domínio precisa resolver para a VM e a porta 80 precisa estar acessível para essa validação. Use https://seu-dominio.com para informar a chave e utilizar o aplicativo publicamente. Enquanto não configurar HTTPS, mantenha o acesso de teste pelo túnel SSH.

## 10. Medir a memória e diagnosticar

```bash
free -h
ps -eo pid,comm,rss --sort=-rss | head -12
sudo systemctl status techmind-python techmind-backend --no-pager
sudo journalctl -u techmind-python -n 80 --no-pager
sudo journalctl -u techmind-backend -n 80 --no-pager
sudo journalctl -k --no-pager | grep -i -E 'out of memory|killed process'
```

RSS é exibido em KiB. Meça em repouso e durante classificações e envio de CSV. Swap utilizada continuamente, reinícios ou erros OutOfMemory indicam que a carga não cabe nessa configuração. Reduza a carga, otimize os serviços ou use mais memória; swap não substitui RAM. Não rode npm start, Maven, múltiplos workers Python ou Oracle Database na VM.

Erros comuns: 502 no Nginx → backend parado; falha Oracle → DB_URL, senha, ACL /32 ou TLS; página abre mas API retorna 401 → chave incorreta; timeout externo → rota da subnet, regra OCI ou firewall do Ubuntu; schema-validation → esquema não foi criado ou diverge da versão do JAR.

Os serviços iniciam após reboot. Para atualizar, compile localmente, envie novos artefatos, substitua os arquivos e reinicie os serviços. Faça backup antes de alterações de esquema. Mantenha validate em atualizações normais e acompanhe os logs.

## Fontes oficiais

- [OCI Always Free: recursos, shapes, volumes e recuperação de instâncias ociosas](https://docs.oracle.com/en-us/iaas/Content/FreeTier/freetier_topic-Always_Free_Resources.htm)
- [Oracle JDBC com TLS sem wallet](https://docs.oracle.com/en/cloud/paas/autonomous-database/serverless/adbsb/connect-jdbc-thin-tls.html)
- [Ubuntu: certificados TLS com Nginx e Certbot](https://ubuntu.com/server/docs/how-to/security/obtain-tls-certificates/)

Confirme a etiqueta Always Free no console e os limites disponíveis na sua conta. Falta de capacidade pode impedir a criação da shape. Instâncias Always Free ociosas podem ser recuperadas pela Oracle; mantenha cópias dos artefatos e dos dados.
