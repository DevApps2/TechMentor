# Guia: rodar o TechMentor inteiro no notebook (API, banco, e-mail, Google Meet e ngrok)

Este guia é para quem vai deixar a API rodando num notebook **sem ninguém por perto**, servindo o app, o site "Seja nosso parceiro" e o portal das parceiras pela internet, usando o ngrok. Siga **na ordem**. Cada parte termina com um teste: só passe para a próxima quando o teste passar.

> **Regra de ouro:** nenhuma senha, chave ou token vai para o GitHub, nem neste guia. Tudo isso fica só no arquivo `backend/rodar-local.ps1` (que o Git ignora) e no banco do notebook.

## Sumário
0. [Como as peças se encaixam](#0-como-as-peças-se-encaixam)
1. [O que o Matheus precisa receber (em canal privado) e o que ele gera sozinho](#1-segredos-quem-fornece-o-quê)
2. [Instalar os programas](#2-instalar-os-programas)
3. [Baixar o projeto](#3-baixar-o-projeto)
4. [Banco de dados](#4-banco-de-dados)
5. [E-mail (Gmail)](#5-e-mail-gmail)
6. [Google Calendar / Meet](#6-google-calendar--meet-sala-da-mentoria)
7. [O script de inicialização](#7-o-script-de-inicialização-todas-as-variáveis)
8. [Ligar a API](#8-ligar-a-api)
9. [ngrok (a API na internet)](#9-ngrok-a-api-na-internet)
10. [Site e portal apontando para a API](#10-site-e-portal-apontando-para-a-api)
11. [Teste final de ponta a ponta](#11-teste-final-de-ponta-a-ponta)
12. [Deixar o notebook funcionando sozinho](#12-deixar-o-notebook-funcionando-sozinho)
13. [Rotina: backup e conferência](#13-rotina-backup-e-conferência)
14. [Problemas comuns](#14-problemas-comuns)

---

## 0. Como as peças se encaixam

```
Celular (app)  ──────┐
Site (GitHub Pages) ─┼──► https://SEU-DOMINIO.ngrok-free.dev ──► ngrok ──► API (porta 8080, neste notebook) ──► MySQL
Portal (Vercel) ─────┘                                                         ├──► Gmail (e-mails)
                                                                               └──► Google Calendar (sala do Meet)
```

- **Este notebook precisa ficar ligado, com internet, a API rodando e o ngrok aberto.** Se qualquer um dos três cair, o app, o site e o portal deixam de funcionar.
- Os endereços de fora (site, portal, app) já apontam para o **domínio do ngrok**. Por isso o ideal é manter **o mesmo domínio** (veja a parte 9).

## 1. Segredos: quem fornece o quê

**Quem tem o projeto hoje (o dono) manda para o Matheus por canal privado (mensagem direta, nunca grupo, nunca GitHub):**

| Item | Para quê |
|---|---|
| Senha de app do Gmail da conta `techmentor.apps@gmail.com` | enviar e-mails (parte 5) |
| `GOOGLE_CLIENT_ID`, `GOOGLE_CLIENT_SECRET` e `GOOGLE_REFRESH_TOKEN` | sala do Meet (parte 6). Se não existirem, o Matheus gera os dele seguindo a parte 6 |
| `authtoken` do ngrok **(só se for usar o mesmo domínio)** | manter o mesmo endereço (parte 9) |

**O Matheus gera sozinho (não precisa de ninguém):**

| Item | Como |
|---|---|
| `JWT_SECRET` | comando na parte 7. **Cada servidor tem o seu** |
| Senha do usuário do MySQL | ele inventa, na parte 4 |

Depois de terminar, **apague as mensagens** com os segredos e **troque a senha de app do Gmail** se achar que vazou.

## 2. Instalar os programas

Abra o **PowerShell** e confira o que já existe. Para cada comando abaixo, se aparecer um número de versão, está instalado:

```powershell
java -version
git --version
mysql --version
ngrok version
node -v
```

O que precisa e como instalar (com `winget`, que já vem no Windows 11; se não tiver, baixe pelo site de cada um):

```powershell
winget install --id Microsoft.OpenJDK.21 -e
winget install --id Git.Git -e
winget install --id ngrok.ngrok -e
winget install --id Oracle.MySQL -e
```

- **Java (JDK) 17 ou superior.** O projeto roda no 17 e no 21. **Feche e abra o PowerShell** depois de instalar.
- **Git.**
- **ngrok.**
- **MySQL 8** (parte 4). Se o `winget` não achar `Oracle.MySQL`, baixe o *MySQL Installer* em mysql.com/downloads e escolha só o **MySQL Server**. Anote a senha do usuário `root` que o instalador pedir.
- **Node.js** só é necessário se este notebook também for **construir ou publicar** o site ou o portal. Para rodar só a API, não precisa.

**Teste:** `java -version` mostra 17 ou mais; `git --version`, `ngrok version` e `mysql --version` mostram versões.

## 3. Baixar o projeto

Escolha uma pasta simples, **sem espaços e sem acento** no caminho (por exemplo `C:\TechMentor-api`):

```powershell
mkdir C:\TechMentor-api
cd C:\TechMentor-api
git clone https://github.com/DevApps2/TechMentor.git
cd TechMentor
git checkout main
git pull
```

Se o repositório for privado, o Git pede login do GitHub (use uma conta com acesso à organização `DevApps2`).

Defina **quem assina os commits** neste computador (para não sair com a conta de outra pessoa). Dentro da pasta `TechMentor`:

```powershell
git config --local user.name "NOME DO MATHEUS NO GITHUB"
git config --local user.email "EMAIL DO GITHUB DELE"
```

**Teste:** a pasta `C:\TechMentor-api\TechMentor\backend` existe e tem o arquivo `mvnw.cmd`.

## 4. Banco de dados

Há duas opções. **Para ficar sem ninguém por perto, use o MySQL** (4A): os dados (pedidos, parceiras, cursos, mentorias) **sobrevivem a reinícios**. O H2 (4B) apaga tudo cada vez que a API reinicia e serve só para demonstração.

### 4A. MySQL (recomendado)

Entre no MySQL como `root` (digite a senha que definiu na instalação):

```powershell
mysql -u root -p
```

Dentro dele (troque `SENHA_FORTE_AQUI` por uma senha inventada por você, e **anote-a** para a parte 7):

```sql
CREATE DATABASE techmentor CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci;
CREATE USER 'techmentor'@'localhost' IDENTIFIED BY 'SENHA_FORTE_AQUI';
GRANT ALL PRIVILEGES ON techmentor.* TO 'techmentor'@'localhost';
FLUSH PRIVILEGES;
EXIT;
```

As tabelas são criadas **sozinhas** na primeira vez que a API sobe (parte 8).

**Dados de partida (conta da administradora e catálogo de cursos).** Um banco novo não tem nenhum usuário, então não dá para entrar como administradora. Depois da **primeira** subida da API (parte 8), rode uma vez, na pasta `TechMentor`:

```powershell
Get-Content backend\src\main\resources\data.sql -Raw -Encoding UTF8 | mysql -u techmentor -p --default-character-set=utf8mb4 techmentor
```

Isso cria:
- a administradora: `admin@techmentor.com`, senha `Techmentor@123`;
- cursos de exemplo (Curso em Vídeo) com as aulas, mentoras, alunas e dados de demonstração.

> **Atenção, segurança:** essa senha é pública (está no repositório). **Entre no app como administradora e troque a senha imediatamente** (tela *Trocar senha*). As contas de demonstração também têm essa senha: se quiser um ambiente "limpo", apague-as do banco depois ou avise a equipe de que elas existem.

### 4B. H2 (só demonstração)

Não instala nada. Em compensação, **todo reinício apaga tudo** e a API sobe sempre com os dados de demonstração. Para usar, no script da parte 7 troque `mysql` por `h2` e ignore as linhas do banco.

**Teste (4A):** `mysql -u techmentor -p -e "SHOW DATABASES;" ` lista o banco `techmentor`.

## 5. E-mail (Gmail)

A API envia e-mails (convite de parceira, confirmação de pedido, recuperação de senha, lembrete de mentoria) pela conta `techmentor.apps@gmail.com`.

1. A conta precisa ter a **verificação em duas etapas** ligada (myaccount.google.com → Segurança).
2. Crie uma **senha de app**: myaccount.google.com/apppasswords → dê um nome (por exemplo "TechMentor API") → o Google mostra **16 letras**. Copie sem espaços. (Se o dono do projeto já gerou, ele manda por canal privado.)
3. Valores para a parte 7:

| Variável | Valor |
|---|---|
| `MAIL_HOST` | `smtp.gmail.com` |
| `MAIL_PORT` | `587` |
| `MAIL_USER` | `techmentor.apps@gmail.com` |
| `MAIL_SENHA` | a senha de app (16 letras) |
| `MAIL_REMETENTE` | `techmentor.apps@gmail.com` |

**Teste** (rode depois de preencher o `rodar-local.ps1`, parte 7; troque o destino por um e-mail seu):

```powershell
& "C:\TechMentor-api\TechMentor\backend\testar-email.ps1" -Para "SEU@EMAIL.COM"
```

Deve chegar um e-mail de teste. Se der erro, veja a tabela de problemas (parte 14).

> Limite do Gmail: cerca de 500 e-mails por dia. Para um piloto é mais que suficiente.

## 6. Google Calendar / Meet (sala da mentoria)

Quando uma mentoria é confirmada, a API cria um evento no Google Calendar da conta do projeto com uma sala do Meet. **Sem estas três variáveis a mentoria funciona normalmente, só que sem link de sala** (a API avisa no log e segue).

### 6.1 Se o dono já tem as três variáveis
Ele manda `GOOGLE_CLIENT_ID`, `GOOGLE_CLIENT_SECRET` e `GOOGLE_REFRESH_TOKEN` por canal privado. **Pule para o teste 6.4.**

### 6.2 Gerar do zero (faça com a conta `techmentor.apps@gmail.com`)

**A) Projeto e API**
1. Acesse console.cloud.google.com, entre com a conta do projeto.
2. Crie um projeto (nome: `TechMentor`).
3. Menu → **APIs e serviços → Biblioteca** → procure **Google Calendar API** → **Ativar**. *(Tem que ser no mesmo projeto das credenciais.)*

**B) Tela de consentimento**
1. **APIs e serviços → Tela de consentimento OAuth** (ou "Google Auth Platform").
2. Tipo de usuário: **Externo**. Preencha nome do app (`TechMentor`) e e-mail de contato.
3. Em **Escopos**, adicione `.../auth/calendar.events`.
4. Em **Usuários de teste**, adicione `techmentor.apps@gmail.com`.
5. **IMPORTANTE — publique o app:** na tela de consentimento, clique em **Publicar app** (status "Em produção"). Se ficar em "Teste", o Google **expira o refresh token em 7 dias** e a sala do Meet volta a dar erro uma semana depois (o erro é `invalid_grant`). Em produção sem verificação, o Google só mostra um aviso de "app não verificado" na hora de autorizar, e o token não expira.

**C) Credencial OAuth**
1. **APIs e serviços → Credenciais → Criar credenciais → ID do cliente OAuth**.
2. Tipo: **Aplicativo da Web**.
3. Em **URIs de redirecionamento autorizados**, adicione exatamente: `https://developers.google.com/oauthplayground`
4. Criar. Copie o **ID do cliente** (`GOOGLE_CLIENT_ID`) e a **chave secreta** (`GOOGLE_CLIENT_SECRET`).

**D) Pegar o refresh token (OAuth Playground)**
1. Acesse developers.google.com/oauthplayground.
2. Engrenagem (canto superior direito) → marque **Use your own OAuth credentials** → cole o Client ID e o Client secret → feche.
3. No campo "Input your own scopes" digite `https://www.googleapis.com/auth/calendar.events` e clique **Authorize APIs**.
4. Entre com `techmentor.apps@gmail.com`. Se aparecer "O Google não verificou este app": **Avançado → Acessar TechMentor (não seguro) → Continuar**.
5. Volte ao Playground e clique **Exchange authorization code for tokens**.
6. Copie o **Refresh token** (`GOOGLE_REFRESH_TOKEN`). Ele só aparece nesse momento: se perder, repita o passo 3.

### 6.3 Convidar os participantes por e-mail?
`GOOGLE_CONVIDAR=true` faz o Google mandar o convite do evento para a aluna e a mentora (é o desejado em produção). Use `false` só em testes com e-mails de mentira.

### 6.4 Teste das credenciais (antes de ligar a API)
Preencha as três variáveis no `rodar-local.ps1` (parte 7) e rode este teste. Ele lê direto do `rodar-local.ps1`, não precisa ligar a API:

```powershell
& "C:\TechMentor-api\TechMentor\backend\testar-google.ps1"
```

Resultado esperado: `OK: o Google aceitou as credenciais e a agenda respondeu.` Se der erro, a mensagem diz o que fazer; a tabela completa está na parte 14.

> **Se aparecer erro de conexão** (`Unexpected end of file`, tempo esgotado, ou o teste nem chega ao Google), o problema **não é o token**: é a **rede** deste notebook (firewall ou filtro de internet de empresa/escola) cortando a conexão com `googleapis.com`. Teste em outra rede (por exemplo o roteador de casa ou o hotspot do celular).

## 7. O script de inicialização (todas as variáveis)

O jeito de não esquecer nenhuma variável é ter **um arquivo só** que as define e liga a API.

1. Na pasta `C:\TechMentor-api\TechMentor\backend`, copie o modelo:

```powershell
cd C:\TechMentor-api\TechMentor\backend
Copy-Item rodar-local.exemplo.ps1 rodar-local.ps1
notepad rodar-local.ps1
```

2. Se o PowerShell reclamar de "a execução de scripts foi desabilitada", libere uma vez (só para o seu usuário) com `Set-ExecutionPolicy -Scope CurrentUser RemoteSigned` e responda `S`.

3. Preencha **todos** os `COLE_AQUI`. O arquivo `rodar-local.ps1` é **ignorado pelo Git** (não vai para o GitHub). Não tire ele dessa pasta nem o envie por mensagem.

4. A chave do login (`JWT_SECRET`): gere uma **nova** com este comando e cole no arquivo (é uma chave por servidor, não precisa ser igual à de ninguém):

```powershell
$b = New-Object byte[] 48; [System.Security.Cryptography.RandomNumberGenerator]::Create().GetBytes($b); [Convert]::ToBase64String($b)
```

Resumo de **todas** as variáveis que o modelo define:

| Variável | O que é | Valor |
|---|---|---|
| `SPRING_PROFILES_ACTIVE` | banco | `mysql` (ou `h2` para demonstração) |
| `SPRING_DATASOURCE_USERNAME` / `SPRING_DATASOURCE_PASSWORD` | usuário do MySQL | `techmentor` / a senha da parte 4 |
| `JWT_SECRET` | chave que assina o login | gerada acima |
| `MAIL_HOST`, `MAIL_PORT`, `MAIL_USER`, `MAIL_SENHA`, `MAIL_REMETENTE` | e-mail | parte 5 |
| `GOOGLE_CLIENT_ID`, `GOOGLE_CLIENT_SECRET`, `GOOGLE_REFRESH_TOKEN` | Meet | parte 6 |
| `GOOGLE_CONVIDAR` | Google convida aluna e mentora | `true` |
| `CORS_ORIGENS` | sites autorizados a chamar a API pelo navegador | só **origens** (sem caminho): `https://devapps2.github.io,https://SEU-PORTAL.vercel.app,http://localhost:5173` |
| `PORTAL_URL` | endereço do portal (vai no link do e-mail de convite) | `https://SEU-PORTAL.vercel.app` |
| `PEDIDOS_EMAIL` | quem recebe o aviso de cada pedido de parceria | `techmentor.apps@gmail.com` |
| `MODERACAO_EMAIL` | quem recebe aviso de denúncia | o e-mail da equipe |
| `PROXY_CONFIAVEL` | a API está atrás do ngrok | `true` |
| `SPRINGDOC_SWAGGER_UI_ENABLED` / `SPRINGDOC_API_DOCS_ENABLED` | esconde a documentação da API na internet | `false` |

## 8. Ligar a API

Na pasta `backend`:

```powershell
.\rodar-local.ps1
```

Na **primeira vez** demora um pouco (baixa as bibliotecas). Está pronta quando aparecer no final:

```
Tomcat started on port 8080 (http) with context path '/techmentor/api'
Started ApiApplication in ... seconds
```

**Deixe essa janela aberta** (fechar a janela desliga a API). Em uma **segunda janela** do PowerShell, teste:

```powershell
curl.exe -s -w "`nstatus=%{http_code}`n" http://localhost:8080/techmentor/api/parceiras/termos
```

Deve mostrar um JSON com `"versao"` e `status=200`.

Se estiver usando MySQL e ainda **não** carregou os dados de partida, **agora é a hora**: rode o comando da parte 4A (carga do `data.sql`).

## 9. ngrok (a API na internet)

O ngrok cria um endereço público `https` que leva até a porta 8080 deste notebook.

### 9.1 Conta e domínio
- **Mesmo domínio de hoje (recomendado):** o domínio é preso à conta do ngrok do dono. O dono manda o **authtoken** por canal privado. Neste notebook:

```powershell
ngrok config add-authtoken COLE_O_AUTHTOKEN_AQUI
```

  **O dono precisa fechar o ngrok no computador dele** antes: o mesmo domínio não pode estar aberto em dois lugares ao mesmo tempo.

- **Domínio novo (conta própria do Matheus):** crie conta em ngrok.com, copie o authtoken do painel e rode o mesmo `ngrok config add-authtoken`. Em *Domains*, crie o domínio gratuito. **Mas aí o endereço muda** e é preciso atualizar os **três lugares** da tabela da parte 10.

### 9.2 Ligar o túnel (em uma terceira janela do PowerShell)

```powershell
ngrok http --url=SEU-DOMINIO.ngrok-free.dev 8080
```

(Se o ngrok disser que `--url` não existe, a versão é mais antiga: use `--domain=SEU-DOMINIO.ngrok-free.dev` no lugar.)

Deixe essa janela aberta também.

**Teste de dentro (no notebook):**

```powershell
curl.exe -s -H "ngrok-skip-browser-warning: 1" https://SEU-DOMINIO.ngrok-free.dev/techmentor/api/parceiras/termos
```

Deve devolver o mesmo JSON de antes.

> **Teste importante, de fora:** abra o mesmo endereço no **celular com dados móveis** (Wi-Fi desligado) ou peça a alguém. Se abrir no celular e não no notebook, é o filtro de internet da rede do notebook bloqueando o ngrok, e isso **não** afeta quem está de fora. Se não abrir em lugar nenhum, o túnel ou a API não estão no ar.

## 10. Site e portal apontando para a API

### Onde o endereço da API aparece (se mudar de domínio, mude nos três)

| Onde | Arquivo | O que trocar | Depois de trocar |
|---|---|---|---|
| Site "Seja nosso parceiro" | repositório `TechMentor-Site`, `js/config.js` → `API_URL` | `https://SEU-DOMINIO.ngrok-free.dev/techmentor/api` | `git add`, `commit`, `push` (o GitHub Pages atualiza em 1 a 2 minutos) |
| Portal das parceiras | variável `VITE_API_URL` na Vercel | o mesmo endereço | publicar o portal de novo |
| App Android | `app/src/main/java/com/example/techmentor/data/ApiClient.kt` → `BASE_URL` (termina com `/`) | `https://SEU-DOMINIO.ngrok-free.dev/techmentor/api/` | gerar um **APK novo** e reinstalar |

Se o domínio do ngrok for **o mesmo de hoje**, **nada disso precisa mudar**.

### Site (GitHub Pages)
- Já publicado em `https://devapps2.github.io/TechMentor-Site/`.
- `js/config.js` precisa de `API_URL` (acima) e `PORTAL_URL` (endereço do portal publicado).
- `CORS_ORIGENS` (parte 7) precisa ter `https://devapps2.github.io`.

### Portal (Vercel) — **necessário para o link do e-mail de convite funcionar**
O e-mail de convite leva a parceira para o portal. Se o portal **não estiver publicado**, o botão do e-mail não abre para ela (ela ainda pode usar o código no aplicativo). Para publicar, **no computador de quem tem o portal** (`techmentor-portal`):

```powershell
npx vercel
npx vercel env add VITE_API_URL production
npx vercel --prod
```

(Use a conta Vercel `techmentorapps-9145`. O valor de `VITE_API_URL` é o endereço da API, terminando em `/techmentor/api`, sem barra no fim.) Anote o endereço final (`https://....vercel.app`) e coloque-o em `PORTAL_URL` e em `CORS_ORIGENS` (parte 7), e no `PORTAL_URL` do `config.js` do site. **Depois reinicie a API.**

## 11. Teste final de ponta a ponta

Faça nesta ordem. Cada item deve dar certo antes do próximo.

1. **API local no ar:** `curl.exe -s -o NUL -w "%{http_code}" http://localhost:8080/techmentor/api/parceiras/termos` → `200`.
2. **API pelo ngrok, de fora** (celular com dados móveis): o endereço da parte 9 abre.
3. **CORS do site** → deve dar `200`:

```powershell
curl.exe -s -o NUL -w "%{http_code}" -X OPTIONS https://SEU-DOMINIO.ngrok-free.dev/techmentor/api/parceiras/pedidos -H "Origin: https://devapps2.github.io" -H "Access-Control-Request-Method: POST" -H "Access-Control-Request-Headers: content-type,ngrok-skip-browser-warning"
```

4. **Entrar no app** como administradora (`admin@techmentor.com`) e **trocar a senha**.
5. **Pedido pelo site:** abra `https://devapps2.github.io/TechMentor-Site/`, preencha com um e-mail **seu**, tipo ONG, envie. Deve aparecer "Recebemos o pedido".
6. **E-mails:** chegou a confirmação no e-mail do pedido, e o aviso em `techmentor.apps@gmail.com`.
7. **Fila:** o aviso aparece no sininho da administradora no app. (A tela da fila de pedidos ainda é feita pelo amigo; até lá, a lista está em `GET /api/admin/pedidos-parceria`, que se vê no Swagger quando ele está ligado, ou pelo banco: `SELECT id, nome, email, situacao FROM pedidos_parceria;`.)
8. **Aprovar** o pedido (`POST /api/admin/pedidos-parceria/{id}/aprovacao`): chega o e-mail de convite com o botão "Criar minha senha" e o código.
9. **Criar a senha** pelo botão (portal) ou pelo código (app), entrar como parceira, criar um curso de teste e enviar para revisão.
10. **Mentoria com sala:** com uma conta de aluna e uma de mentora, marcar e confirmar uma mentoria. A sala do Meet deve aparecer. (Se não aparecer, olhe o log da API: a causa vem escrita. Tabela na parte 14.)

## 12. Deixar o notebook funcionando sozinho

Como ninguém vai estar perto, ajuste o Windows para ele **não dormir nem desligar a internet**. No PowerShell **como administrador**:

```powershell
# Não dormir nem hibernar na tomada
powercfg /change standby-timeout-ac 0
powercfg /change hibernate-timeout-ac 0
# Fechar a tampa NÃO faz nada (na tomada)
powercfg /setacvalueindex SCHEME_CURRENT SUB_BUTTONS LIDACTION 0
powercfg /setactive SCHEME_CURRENT
```

Além disso:
- **Deixe na tomada**, com o carregador firme.
- **Wi-Fi:** desmarque "desligar este dispositivo para economizar energia" nas propriedades do adaptador de rede (Gerenciador de Dispositivos).
- **Atualizações do Windows:** reiniciam o notebook sozinhas. Em *Configurações → Windows Update → Opções avançadas*, defina o **horário ativo** para cobrir o dia todo, e **depois de qualquer reinício** religue a API e o ngrok (ou use as tarefas automáticas abaixo).
- **Notificações e proteção de tela:** desligue o bloqueio por inatividade, ou o ngrok e a API continuam rodando mas ninguém consegue "ver" o notebook para consertar. (Rodar é independente de tela.)

### Ligar sozinho quando o notebook reiniciar (opcional, mas recomendado)
No PowerShell **como administrador**, ajustando o caminho:

```powershell
schtasks /Create /TN "TechMentor API" /SC ONLOGON /TR "powershell -NoProfile -ExecutionPolicy Bypass -File C:\TechMentor-api\TechMentor\backend\rodar-local.ps1" /F
schtasks /Create /TN "TechMentor ngrok" /SC ONLOGON /DELAY 0000:30 /TR "ngrok http --url=SEU-DOMINIO.ngrok-free.dev 8080" /F
```

Isso só funciona depois que **a pessoa entrar no Windows** (login). Deixe o notebook **sem senha na tela de entrada** ou com login automático se ele precisar voltar sozinho depois de uma queda de energia.

## 13. Rotina: backup e conferência

**Backup do banco (o mais importante), por exemplo uma vez por semana:**

```powershell
mkdir C:\backup-techmentor -ErrorAction SilentlyContinue
mysqldump -u techmentor -p --default-character-set=utf8mb4 techmentor > C:\backup-techmentor\banco-$(Get-Date -Format yyyy-MM-dd).sql
```

**Copie também estas duas pastas** (fotos de perfil e currículos em PDF), que ficam em `backend`:

```powershell
Copy-Item C:\TechMentor-api\TechMentor\backend\fotos C:\backup-techmentor\fotos -Recurse -Force
Copy-Item C:\TechMentor-api\TechMentor\backend\curriculos C:\backup-techmentor\curriculos -Recurse -Force
```

Leve a pasta `C:\backup-techmentor` para **fora do notebook** (pen drive ou nuvem). Backup que fica só no mesmo computador não protege de nada.

**Conferência rápida (1 minuto):** o endereço do ngrok abre no celular com dados móveis? A janela da API está aberta e sem erros repetidos? O disco tem espaço (`Get-PSDrive C`)?

## 14. Problemas comuns

| O que aparece | Causa provável | O que fazer |
|---|---|---|
| `Unexpected end of file from server`, tempo esgotado, ou `Falha ao criar a sala do Google Meet` sem outra explicação | **Rede** do notebook (filtro de internet de empresa/escola) cortando a conexão com o Google | Testar em outra rede (casa ou hotspot do celular). Pedir à TI para liberar `*.googleapis.com` e `oauth2.googleapis.com` |
| `invalid_grant` | Refresh token **vencido ou revogado**. Acontece depois de 7 dias se o app Google ficou em "Teste", ou se a senha da conta foi trocada | Parte 6.2-B (publicar o app) e 6.2-D (gerar um token novo). Atualize `GOOGLE_REFRESH_TOKEN` e reinicie a API |
| `invalid_client` ou `unauthorized_client` | `GOOGLE_CLIENT_ID` ou `GOOGLE_CLIENT_SECRET` errados, ou copiados com espaço no fim | Copiar de novo da tela de credenciais |
| `403 ... Calendar API has not been used` / `accessNotConfigured` | A API do Calendar não foi ativada **no mesmo projeto** das credenciais | Parte 6.2-A, passo 3 |
| `403 insufficientPermissions` | O escopo do token não cobre eventos | Gerar o token de novo com o escopo `https://www.googleapis.com/auth/calendar` |
| `redirect_uri_mismatch` (no Playground) | O endereço do Playground não foi adicionado na credencial | Parte 6.2-C, passo 3 (exatamente `https://developers.google.com/oauthplayground`) |
| `PKIX path building failed` | A rede "abre" o tráfego seguro (inspeção SSL) e o Java não confia no certificado | Usar outra rede, ou instalar o certificado da empresa no Java (peça à TI) |
| A mentoria foi criada mas sem link de sala | O Google falhou ou as variáveis estão vazias | Ver o log da API na hora da confirmação; a mentoria continua válida |
| E-mail não sai: `Authentication failed` / `535` | Senha de app errada, ou verificação em duas etapas desligada | Parte 5: gerar uma senha de app nova, sem espaços |
| E-mail não sai: `Could not connect to SMTP host` / tempo esgotado | A rede bloqueia a porta 587 | Usar outra rede |
| O formulário do site dá "Não foi possível conectar ao servidor" | CORS sem o endereço do site, ou ngrok desligado, ou a API desligada | Parte 11, itens 1 a 3. Confira `CORS_ORIGENS` (só origens, sem caminho) e **reinicie a API** depois de mudar |
| O navegador mostra uma página HTML do ngrok em vez do JSON | O túnel mostra um aviso para navegadores | Os clientes do projeto já enviam o cabeçalho `ngrok-skip-browser-warning`; se testar à mão, mande esse cabeçalho |
| `ERR_NGROK_8012` ou `ERR_NGROK_3200` | A API não está respondendo na porta 8080, ou o túnel está fechado | Subir a API (parte 8) e o ngrok (parte 9) |
| `429` ao enviar pedido | O mesmo endereço mandou mais de 5 pedidos em 1 hora | Esperar uma hora. É proposital, contra robôs |
| A API não sobe: `Access denied for user` | Usuário ou senha do MySQL errados | Conferir `SPRING_DATASOURCE_USERNAME` e `SPRING_DATASOURCE_PASSWORD` |
| `a execução de scripts foi desabilitada neste sistema` ao rodar um `.ps1` | Política de execução do Windows | `Set-ExecutionPolicy -Scope CurrentUser RemoteSigned` |
| A API não sobe: `JWT_SECRET` | Falta a chave do login | Preencher no `rodar-local.ps1` |
| A API não sobe: `Port 8080 was already in use` | Já há uma API aberta | Fechar a outra janela, ou `netstat -ano | findstr :8080` e encerrar o processo |
| Depois de reiniciar, tudo sumiu | Está usando o perfil `h2` | Usar `mysql` (parte 4A) |
| Tudo parou de noite | O notebook dormiu, ou o Windows reiniciou | Parte 12 |
| O link do e-mail de convite abre uma página que não existe | `PORTAL_URL` aponta para `localhost`, ou o portal não foi publicado | Parte 10 (Portal na Vercel) e ajustar `PORTAL_URL` |

**Olhar o log:** tudo o que a API faz aparece na janela onde ela está rodando. Procure linhas com `ERROR` ou `WARN`. As falhas do Google e do e-mail **nunca derrubam** a API: são só avisos.

---

### Lista final de conferência (imprima ou marque)

- [ ] Java, Git, MySQL e ngrok instalados
- [ ] Projeto baixado na branch `main`
- [ ] Banco `techmentor` e usuário criados; dados de partida carregados; senha da administradora **trocada**
- [ ] Senha de app do Gmail testada (`testar-email.ps1`)
- [ ] Credenciais do Google testadas (`testar-google.ps1`) e app Google **publicado em produção**
- [ ] `rodar-local.ps1` preenchido, API sobe e responde `200`
- [ ] ngrok no ar e o endereço abre **pelo celular com dados móveis**
- [ ] CORS testado com `200`
- [ ] Pedido de teste pelo site chegou, com os dois e-mails
- [ ] Aprovação gera o convite
- [ ] Notebook configurado para não dormir; tarefas de inicialização criadas
- [ ] Backup feito pelo menos uma vez e guardado fora do notebook
