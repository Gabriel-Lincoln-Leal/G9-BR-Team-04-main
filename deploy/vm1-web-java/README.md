# VM 1 — Nginx, Angular e Java

Manter a VM atual: `140.238.236.255`. Sistema Ubuntu 24.04, Java 21, Nginx e Certbot. Manter os limites JVM existentes e a swap de 2 GB.

## Arquivos a instalar

- JAR construído de `backend/`: `/opt/techmind/techmind.jar`.
- Build de `frontend/`: diretório de frontend da release, servido pelo Nginx.
- Configuração protegida: `/etc/techmind/techmind.env`.
- Unidade Java: `/etc/systemd/system/techmind-backend.service`.

## Rede

- Entrada pública TCP 80/443.
- SSH 22 limitado às origens administrativas quando possível.
- Java em `127.0.0.1:8080`, sem entrada pública nessa porta.
- Saída para o IP privado da VM 2, TCP 8000.
- Conexão Oracle existente por TCPS 1521.

## Mudanças necessárias na migração

1. Preservar as credenciais e o certificado existentes.
2. Definir `CLASSIFIER_BASE_URL=http://IP_PRIVADO_VM2:8000` no arquivo de ambiente; substituir o marcador pelo IP real.
3. Remover `techmind-python.service` das linhas `Wants` e `After` da unidade Java, preservando `network-online.target`: Python passará a executar remotamente.
4. Executar `sudo systemctl daemon-reload` após alterar a unidade.
5. Reiniciar o backend e testar classificação e consulta pelo site.
6. Somente após sucesso, executar `sudo systemctl disable --now techmind-python` na VM 1.

O Nginx continua encaminhando a API ao Java local. Não é necessário alterar a ACL Oracle, pois o Java permanece nesta VM. Não execute o provisionador de banco novamente.

Se a validação falhar, restaure o endpoint local, a unidade anterior e o serviço Python antigo. Preserve uma cópia das configurações antes de modificá-las.
