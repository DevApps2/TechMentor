# TechMentor

Aplicativo Android com uma API própria para **conectar mulheres que estão entrando em tecnologia a mentoras, cursos e oportunidades**. A usuária estuda, ganha XP e conquistas, marca mentorias por videochamada, se candidata a vagas e eventos, e conta com ferramentas de segurança (denúncia e bloqueio). Uma administradora modera o conteúdo e acompanha tudo por um painel dentro do próprio app.

> Este repositório tem duas partes: o **aplicativo** (`app/`) e a **API** (`backend/`).

---

## O que o TechMentor faz

| Área | O que a usuária consegue fazer |
|---|---|
| **Aprendizado** | Catálogo de cursos por categoria, trilha com módulos e aulas (concluir a aula libera a próxima), progresso por curso, XP, níveis, sequência de dias, conquistas e desafios |
| **Mentoria** | Ver mentoras por área, agendar horário, confirmar, reagendar e cancelar; sala do **Google Meet** que abre 30 minutos antes; lembretes e e-mails; pedidos não confirmados expiram sozinhos |
| **Avaliações** | Aluna e mentora se avaliam de 1 a 5 estrelas em até 14 dias; nota média pública da mentora; comentários podem ser denunciados |
| **Oportunidades** | Estágios, empregos, bolsas e eventos; busca e filtros; candidatura ou inscrição, retirada da candidatura e acompanhamento da situação (enviada, em análise, aprovada, recusada) |
| **Segurança** | Denúncia de usuárias e de comentários, bloqueio, orientações e canais de ajuda |
| **Conta** | Cadastro, login, recuperação de senha por código no e-mail, troca de senha, foto e perfil; a mentora cadastrada pela administradora entra com uma senha provisória e é levada a escolher a própria no primeiro acesso |
| **Notificações** | Sininho dentro do app e e-mails padronizados |
| **Administração** | Painel para denúncias (inclusive esconder comentários), mentoras, oportunidades e candidaturas |

---

## Tecnologias

**Aplicativo**
- Kotlin e Jetpack Compose (Material 3), Navigation Compose
- Retrofit, OkHttp e Gson; Coil para imagens
- Android 7.0 ou superior (minSdk 24)

**API**
- Java 17 (roda também no 21) e Spring Boot 4.1
- Spring Web, Spring Data JPA, Bean Validation
- MySQL 8 (padrão) ou H2 em memória (desenvolvimento e testes)
- Spring Security com **JWT** (JJWT)
- Spring Mail, Google Calendar API (sala do Meet), SpringDoc/Swagger
- Maven (use o `mvnw` que já vem no projeto)

---

## Estrutura do repositório

```text
TechMentor/
├── app/        # aplicativo Android (Kotlin + Compose)
│   └── src/main/java/com/example/techmentor/
│       ├── data/        # chamadas à API (Retrofit), modelos e sessão
│       ├── navigation/  # rotas do app
│       └── ui/          # telas por área (home, mentoria, oportunidades, admin...)
└── backend/    # API (Spring Boot)
    ├── src/main/java/com/techmentor/api/
    │   ├── controller/  # endpoints
    │   ├── service/     # regras de negócio
    │   ├── repository/  # acesso ao banco
    │   ├── model/       # entidades
    │   ├── dto/         # contratos de entrada e saída
    │   ├── security/    # JWT
    │   ├── evento/      # eventos e notificações
    │   ├── email/       # modelo dos e-mails
    │   └── sala/        # criação da sala do Meet
    ├── src/main/resources/   # configurações e data.sql (dados de teste)
    └── db/                   # scripts para atualizar um MySQL existente
```

Mais detalhes da organização do backend em [`backend/README-arquitetura.md`](backend/README-arquitetura.md).

---

## Como rodar

### 1) A API

Pré-requisito: **Java 17 ou superior**. Os comandos abaixo são para a pasta `backend/` (no Windows use `mvnw.cmd`; no Linux e no macOS, `./mvnw`).

**Variável obrigatória.** A API não sobe sem a chave que assina o login:

```bash
# Gere uma chave (Base64, 32 bytes ou mais) e guarde-a só na sua máquina
# PowerShell:
$b = New-Object byte[] 48; [System.Security.Cryptography.RandomNumberGenerator]::Create().GetBytes($b); [Convert]::ToBase64String($b)
```

Defina o resultado em `JWT_SECRET` (variável de ambiente ou configuração de execução do IntelliJ).

**Modo mais simples: banco em memória (H2).** Não precisa instalar nada. A cada subida o banco nasce novo, já com dados de teste (`data.sql`):

```bash
./mvnw.cmd spring-boot:run -Dspring-boot.run.profiles=h2
```

**Com MySQL.** Crie o banco `techmentor`, ajuste `application-mysql.properties` (usuário e senha) e rode sem perfil (o padrão é `mysql`). As tabelas são criadas sozinhas; para carregar os dados de teste, execute o `data.sql` uma vez.

A API fica em `http://localhost:8080/techmentor/api` e a documentação interativa (Swagger) em `http://localhost:8080/techmentor/api/swagger-ui.html`.

**Outras variáveis (opcionais):**

| Variável | Para quê |
|---|---|
| `MAIL_HOST`, `MAIL_PORT`, `MAIL_USER`, `MAIL_SENHA`, `MAIL_REMETENTE` | Envio de e-mails (recuperação de senha, lembretes, avisos). Sem elas, o envio falha, mas o app funciona |
| `GOOGLE_CLIENT_ID`, `GOOGLE_CLIENT_SECRET`, `GOOGLE_REFRESH_TOKEN` | Sala do Google Meet nas mentorias. Sem elas, a mentoria funciona, só que sem link |
| `GOOGLE_CONVIDAR` | `false` para o Google não mandar convites por e-mail (útil com usuárias de teste) |
| `MODERACAO_EMAIL` | Para onde vai o aviso de nova denúncia |
| `SQL_VISIVEL` | `true` mostra o SQL no console |
| `CADASTRO_MAX_POR_HORA` | Limite de cadastros por hora por endereço (padrão 10) |
| `APP_FUSO` | Fuso horário do TechMentor (padrão `America/Sao_Paulo`). O servidor passa a usar esse relógio e as datas da API saem com o deslocamento, por exemplo `2026-11-10T14:00:00-03:00` |
| `PROXY_CONFIAVEL` | `true` quando a API está atrás de um proxy (ngrok, nginx); `false` se estiver exposta direto |

> **Nunca coloque senhas, tokens ou chaves no código nem no GitHub.** Tudo isso vem de variáveis de ambiente.

### 2) O aplicativo

1. Abra a **pasta raiz** do projeto no Android Studio e espere o Gradle sincronizar.
2. Em `app/src/main/java/com/example/techmentor/data/ApiClient.kt`, ajuste o `BASE_URL` (precisa terminar com `/`):

| Onde o app roda | `BASE_URL` |
|---|---|
| Emulador | `http://10.0.2.2:8080/techmentor/api/` |
| Celular na mesma rede Wi-Fi | `http://IP-DO-SEU-COMPUTADOR:8080/techmentor/api/` |
| Celular fora da sua rede | um túnel como o **ngrok** (`ngrok http 8080`) e a URL `https://...ngrok-free.dev/techmentor/api/` |

3. Rode o app (**Run**). Com o ngrok, o computador precisa ficar ligado, com a API e o túnel abertos.

---

## Contas de teste

Com o perfil `h2` (ou depois de carregar o `data.sql`), todas usam a senha **`Techmentor@123`**:

| Papel | E-mail |
|---|---|
| Administradora | `admin@techmentor.com` |
| Mentoras | `maria.silva@techmentor.com`, `juliana.costa@techmentor.com`, `fernanda.alves@techmentor.com` |
| Alunas | `beatriz.santos@email.com`, `camila.rocha@email.com`, `daniela.martins@email.com`, `elisa.ferreira@email.com` |

O final do `data.sql` monta um **cenário de teste com datas relativas a "agora"**: pedidos de mentoria aguardando confirmação (um já vencido, que expira sozinho em instantes), mentorias confirmadas (uma começando em 20 minutos), concluídas, avaliações, denúncias, notificações e candidaturas em todas as situações. Os dados desse cenário começam com `[TESTE]` e são recriados a cada execução.

> Os e-mails dessas contas **não existem de verdade**. Para testar o envio de e-mail, cadastre uma conta com um endereço real seu.

---

## Segurança

- **Login com JWT.** O servidor identifica a pessoa só pelo token (`Authorization: Bearer ...`); nenhuma rota confia em um id enviado pelo app.
- **Papéis** `ALUNA`, `MENTORA` e `ADMIN`. As regras por rota ficam em `SecurityConfig`: gestão de conteúdo só para a administradora; notificações, progresso, matrículas e outros dados pessoais só da própria dona; regras que dependem do dado (por exemplo, "esse agendamento é seu?") ficam nos serviços.
- **Senhas** com BCrypt. Trocar ou redefinir a senha **derruba os tokens antigos**.
- **Senha provisória.** A mentora cadastrada pela administradora entra com a senha que ela definiu; o login devolve `precisaTrocarSenha: true` e o app leva a mentora direto para a troca de senha (sem voltar) antes de qualquer outra tela. A marca cai quando ela troca ou redefine a senha.
- **Limites** contra abuso: tentativas de login e de código de recuperação, cadastros por endereço e denúncias.
- **Fotos de perfil** validadas pelo conteúdo do arquivo (JPG, PNG ou WEBP de verdade).
- **Erros** sempre no formato `{"erro": "mensagem"}`, sem detalhes internos.

---

## Testes

Na pasta `backend/`:

```bash
./mvnw.cmd test
```

A suíte (cerca de 70 testes) roda com um banco H2 em memória, sem precisar de MySQL, e cobre login e recuperação de senha, regras de acesso, agendamento e expiração, avaliações e prazo, denúncias e moderação, oportunidades e candidaturas, e o carregamento do `data.sql`.

---

## Publicando a API

O perfil `prod` (`application-prod.properties`) desliga o Swagger, só confere o banco (sem alterá-lo) e esconde detalhes de erro. Ative com `SPRING_PROFILES_ACTIVE=mysql,prod`. Antes de publicar:

- defina uma `JWT_SECRET` **própria do servidor** (diferente da de desenvolvimento);
- coloque a API atrás de **HTTPS** (por exemplo, um proxy reverso com certificado);
- deixe `MAIL_REDIRECIONAR_PARA` vazia e use as credenciais reais de e-mail;
- se o banco já existir, rode o que estiver comentado no fim de `backend/db/atualizar-mysql.sql`.

---

## Documentação da API

Com a API rodando, o Swagger lista todas as rotas, os campos de cada uma e permite testá-las. Para chamar uma rota protegida, faça `POST /usuarios/login`, copie o `accessToken` e informe-o no botão **Authorize**.
