# Implantação realizada

- Site: https://techmind.140-238-236-255.sslip.io
- VM: 140.238.236.255, Ubuntu 24.04, VM.Standard.E2.1.Micro, 1 GB.
- Banco: Autonomous Database Always Free apidb, usuário da aplicação TECHMIND.
- ACL: acesso ao banco restrito ao IP público da VM; TLS permitido, mTLS não obrigatório.
- Serviços ativos e habilitados: techmind-python, techmind-backend, nginx.
- Java e Python escutam somente em loopback; o Nginx publica o site com HTTPS.
- Configuração sensível: /etc/techmind/techmind.env, root, modo 600. Nenhuma senha foi salva neste repositório.
- DB_DDL_AUTO=validate após criação inicial do esquema.
- Certificado Let's Encrypt com renovação automática via certbot.timer; teste de renovação concluído com sucesso.
- Senha ADMIN temporária removida da VM após configurar o usuário da aplicação.

Validações realizadas: página Angular HTTPS 200; API sem chave 401; classificação e persistência 201; listagem 200; backend health UP; modelo Python carregado. Um conteúdo identificado como teste de implantação foi mantido no banco (ID 1).

Na última medição, o sistema usou aproximadamente 718 MiB de RAM, com 235 MiB disponíveis e 169 MiB de swap usados. Isso é uma medição de teste, não uma validação de capacidade sob carga.

O endereço gratuito depende do serviço DNS sslip.io e do IP público atual. Se o IP mudar, atualize o endereço, a ACL do banco e o certificado.

Para consultar a chave no próprio computador, use PowerShell, sem imprimir seu valor:

```powershell
$applicationKey = (& ssh -i 'C:\Users\admin\.ssh\ssh-key-2026-10-02.key' ubuntu@140.238.236.255 "sudo sed -n 's/^TECHMIND_API_KEY=//p' /etc/techmind/techmind.env").Trim()
Set-Clipboard -Value $applicationKey
```

Cole no campo de chave de acesso do site. Para logs e diagnóstico:

```bash
sudo systemctl status techmind-backend techmind-python nginx
sudo journalctl -u techmind-backend -n 80 --no-pager
sudo journalctl -u techmind-python -n 80 --no-pager
free -h
```
