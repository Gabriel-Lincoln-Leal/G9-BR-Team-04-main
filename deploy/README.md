# Implantação OCI

Organização para a implantação manual em duas VMs. A migração ainda não foi executada.

```text
deploy/
├── vm1-web-java/       Requisitos da VM pública: Nginx, Angular e Java
├── vm2-python/         Requisitos da VM do classificador
├── checks/             Verificações Java/Python
├── database/           Auxiliares JDBC de conexão e provisionamento
├── docs/               Guia de estudos, relatório e status histórico
├── legacy-single-vm/   Scripts da instalação anterior em uma VM
├── artifacts/          ZIP e staging locais, ignorados pelo Git
└── evidence/           Capturas da implantação, ignoradas pelo Git
```

## Arquitetura proposta

Internet → VM 1 (Nginx + Angular + Java) → VM 2 (Python, pela rede privada).
O Java da VM 1 continua acessando o Autonomous Database. A ACL do banco permanece associada à VM 1.

## Código-fonte e builds

- `../backend/`: Java; build com Maven; JAR em `target/`.
- `../frontend/`: Angular; build com npm; arquivos em `dist/`.
- `../api-python/`: código e modelos Python; instalar dependências no Linux.

Os diretórios por VM descrevem a instalação; não duplicam o código-fonte. Os caminhos de build foram preservados.

## Por onde começar

1. Leia [VM 1](vm1-web-java/README.md) e [VM 2](vm2-python/README.md).
2. Crie a segunda VM na mesma VCN e registre o IP privado.
3. Instale e valide o classificador novo antes de alterar o Java.
4. Ajuste o endpoint do classificador na VM 1 e teste pelo site.
5. Desative o Python antigo somente depois da validação.

O [guia de estudos](docs/GUIA_ESTUDOS_DEVOPS_INFRA_CLOUD.md), o [relatório](docs/RELATORIO_IMPLANTACAO_OCI.md) e o [status](docs/STATUS_OCI.md) descrevem a implantação anterior, com uma VM. Não são evidência de uma migração já realizada.

Os scripts em `legacy-single-vm` são de bootstrap: podem substituir configurações. `database/DbProvision.java` altera a senha do usuário TECHMIND quando executado novamente. `checks/check-backend.py` pode gravar conteúdo de teste.
