.

## O que já está no projeto

- Aplicativo Android e API no mesmo repositório (`app/` e `backend/`).
- Painel administrativo para convidar, verificar, ativar, suspender e reativar parceiras.
- Aceite do convite, leitura dos termos e criação de senha pela pessoa responsável.
- Cadastro e envio de cursos para revisão, com notificações de parceria usando o serviço de e-mail existente.
- Confirmações e lembretes de mentoria existentes. Eles continuam usando o mesmo `EmailService`; não é necessário criar outro sistema.

## 1. Configurar e testar o Mailtrap

O backend já usa o host SMTP do Mailtrap como padrão (`sandbox.smtp.mailtrap.io`, porta `2525`). As credenciais SMTP não estão preenchidas neste ambiente. Para enviar mensagens ao inbox de testes do Mailtrap:

1. Crie ou abra um inbox no Mailtrap e copie as credenciais SMTP.
2. Configure as variáveis abaixo na configuração de execução do backend. No PowerShell, elas podem ser definidas antes de iniciar a API:

   ```powershell
   $env:MAIL_HOST = "sandbox.smtp.mailtrap.io"
   $env:MAIL_PORT = "2525"
   $env:MAIL_USER = "COLE_O_USUARIO_SMTP_DO_MAILTRAP"
   $env:MAIL_SENHA = "COLE_A_SENHA_SMTP_DO_MAILTRAP"
   $env:MAIL_REMETENTE = "nao-responda@techmentor.com"
   ```

3. Reinicie a API e faça um convite de parceria pelo painel admin.
4. Confira o e-mail e o código no inbox do Mailtrap.
5. Teste também os e-mails de mentoria. Como todos passam pelo mesmo serviço SMTP, em ambiente de teste também serão capturados pelo Mailtrap.

Não coloque as credenciais no código, neste README ou no GitHub. As variáveis definidas no PowerShell valem apenas para aquela janela. Para uso contínuo, configure-as como variáveis de ambiente do usuário/servidor ou na plataforma onde a API será executada.

## 2. Revisar os termos de parceria

O texto exibido no app fica em [`TermosDeParceria.java`](backend/src/main/java/com/techmentor/api/service/TermosDeParceria.java). Ele está marcado no código como texto-base e precisa de revisão jurídica antes de convidar empresas reais. O documento com regras do módulo está em [`contrato-parceiras-e-cursos.md`](backend/docs/contrato-parceiras-e-cursos.md).

Se o texto dos termos mudar, atualize a versão junto com o texto no código para que os novos aceites fiquem registrados com a versão correta.

## 3. Rodar a API localmente

Pré-requisitos: Java 17 ou superior e PowerShell aberto na pasta `backend/`.

1. Gere uma chave JWT para desenvolvimento e defina-a na sessão atual:

   ```powershell
   $bytes = New-Object byte[] 48
   [System.Security.Cryptography.RandomNumberGenerator]::Create().GetBytes($bytes)
   $env:JWT_SECRET = [Convert]::ToBase64String($bytes)
   ```

2. Configure as variáveis do Mailtrap da seção anterior, se quiser receber os e-mails no inbox de testes.
3. Inicie a API com o banco H2 de desenvolvimento:

   ```powershell
   .\mvnw.cmd spring-boot:run -Dspring-boot.run.profiles=h2
   ```

O H2 é recriado ao reiniciar a API e carrega os dados de teste. Não use essas contas nem esse banco para produção.

## 4. Validar o fluxo de parceria

1. Abra o app com o backend local e entre como admin usando a conta de teste descrita no [`README.md`](README.md).
2. Abra **Gerenciar parceiras** e convide uma instituição.
3. Confira se o e-mail com o código chegou ao Mailtrap.
4. Saia da conta, escolha **Recebi um convite de parceria**, informe o e-mail e o código, leia e aceite os termos e crie a senha.
5. Entre novamente como admin, confira os dados da instituição e conclua a lista de verificação para ativá-la.
6. Entre como parceira, cadastre um curso e envie-o para revisão.
7. No painel admin, teste aprovação, devolução com comentários e recusa com motivo; confira os e-mails e os registros de envio.

Use endereços de teste controlados por você. O Mailtrap Sandbox captura as mensagens de teste; ele não serve como confirmação de entrega para clientes reais.

## 5. Preparar o backend para produção

Antes de publicar a API:

- Crie um banco MySQL de produção e configure URL, usuário e senha próprios. O arquivo local [`application-mysql.properties`](backend/src/main/resources/application-mysql.properties) aponta para `localhost` e não está pronto para produção.
- Faça backup do banco e aplique, com revisão, as atualizações necessárias em [`atualizar-mysql.sql`](backend/db/atualizar-mysql.sql). O perfil `prod` valida o schema e não cria/atualiza tabelas automaticamente.
- Configure no servidor `SPRING_PROFILES_ACTIVE=mysql,prod` e uma `JWT_SECRET` nova, forte e exclusiva de produção.
- Configure `MAIL_HOST`, `MAIL_PORT`, `MAIL_USER`, `MAIL_SENHA` e `MAIL_REMETENTE` na plataforma do servidor. Para produção, use um serviço/inbox de envio adequado a destinatários reais; o Mailtrap Sandbox é para testes.
- Mantenha `MAIL_REDIRECIONAR_PARA` vazia em produção para que os e-mails não sejam redirecionados.
- Se as salas do Google Meet forem usadas, configure `GOOGLE_CLIENT_ID`, `GOOGLE_CLIENT_SECRET` e `GOOGLE_REFRESH_TOKEN`. Em testes, avalie `GOOGLE_CONVIDAR=false` para evitar convites de calendário para endereços de teste.
- Publique atrás de HTTPS e confirme que o servidor, o banco e o armazenamento de fotos estão acessíveis.

## 6. Apontar e publicar o app Android

O endereço atual em [`ApiClient.kt`](app/src/main/java/com/example/techmentor/data/ApiClient.kt) é um túnel ngrok de desenvolvimento. Antes de distribuir o app, substitua `BASE_URL` pela URL HTTPS permanente da API, terminando em `/techmentor/api/`.

Depois:

1. Abra a raiz do projeto no Android Studio e aguarde a sincronização do Gradle.
2. Rode o app e repita os fluxos de login, convite/cadastro da parceira, revisão de curso e mentoria.
3. Gere e guarde com segurança a chave de assinatura do Android; use-a para gerar o APK/AAB de distribuição.
4. Faça a publicação na loja ou distribua o pacote pelo canal escolhido.

## 7. Sobre o site

Este repositório contém o app Android e a API; não há um frontend web separado aqui. Para publicar um site, ainda é preciso ter o código do site e escolher onde hospedá-lo. Nenhum segundo repositório foi criado.

## 8. Antes de concluir

- Rode a suíte do backend na pasta `backend/` com `.\mvnw.cmd test`.
- Compile e execute o app no Android Studio.
- Confirme que a API, o banco, os e-mails do Mailtrap e o fluxo completo de revisão estão funcionando.
- Revise as alterações locais e então faça commit e push para o repositório existente.

