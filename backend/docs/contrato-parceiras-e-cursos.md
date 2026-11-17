# Contrato da API: parceiras, gestão de cursos, revisão e notificações

Para quem faz as telas de administradora e de parceira. Tudo aqui já existe no backend.
A documentação interativa (Swagger) continua em `/swagger-ui.html` e tem os mesmos campos.

- Base: `/techmentor/api` (mesmo prefixo do resto da API). Os caminhos abaixo vêm depois dele.
- Login: o de sempre, `POST /usuarios/login`. A parceira volta com `tipo: "PARCEIRA"`. **O app precisa aceitar esse valor novo** no enum de tipo de usuária (hoje: ALUNA, MENTORA, ADMIN).
- Cabeçalho: `Authorization: Bearer <token>`.
- Erros: sempre `{ "erro": "...", "codigo": ..., "campos": {...} }`. 401 sem login, 403 sem permissão, 404 não achou, 409 conflito de regra, 400 dado inválido.
- Datas: com fuso (`2026-11-14T10:30:00-03:00`).

## 1. Quem acessa o quê

| Papel | O que usa |
|---|---|
| Sem login | `GET /parceiras/termos`, `POST /parceiras/convite/aceitar` |
| PARCEIRA | `/api/parceira/**`, a própria conta (`/usuarios/{id}`, troca de senha, foto) e `/notificacoes/...`. **Todo o resto do app responde 403** (catálogo, matrículas, mentorias...) |
| ADMIN | `/api/admin/**` |
| ALUNA / MENTORA | o app de sempre; `/api/parceira/**` e `/api/admin/**` respondem 403 |

## 2. A vida de uma parceira

```
ADMIN convida ──► e-mail com código ──► PARCEIRA cria a senha e aceita os termos
                                              │ (situação continua CONVIDADA)
                                              ▼
                              ADMIN verifica com o checklist ──► ATIVA ──► cria cursos
                                                                  │
                                              ADMIN suspende ◄────┘ ──► SUSPENSA (só lê)
                                              ADMIN reativa  ──► ATIVA
```

- **CONVIDADA**: recebeu o convite. Depois de aceitar, entra no app, vê o próprio perfil, mas **não cria curso** (403 "Sua conta ainda está em verificação...").
- **ATIVA**: monta cursos e envia para revisão.
- **SUSPENSA**: lê tudo, não altera nada (403). O que já estava publicado continua no ar até a administradora decidir.

## 3. Telas da administradora (`/api/admin/parceiras`)

| Ação | Chamada | Corpo | Resposta |
|---|---|---|---|
| Listar | `GET /api/admin/parceiras?situacao=CONVIDADA` | | lista de `Parceira` (filtro opcional) |
| Detalhar | `GET /api/admin/parceiras/{id}` | | `Parceira` |
| Convidar | `POST /api/admin/parceiras` | `ConviteRequest` | 201 `Parceira` |
| Reenviar convite | `POST /api/admin/parceiras/{id}/convite` | | 204 |
| Verificar | `PUT /api/admin/parceiras/{id}/verificacao` | `VerificacaoRequest` | `Parceira` (ATIVA) |
| Suspender | `POST /api/admin/parceiras/{id}/suspensao` | `{ "motivo": "..." }` | `Parceira` |
| Reativar | `POST /api/admin/parceiras/{id}/reativacao` | | `Parceira` |
| Histórico | `GET /api/admin/parceiras/{id}/historico` | | lista de `Registro` |

**ConviteRequest**
```json
{
  "nome": "Curso em Vídeo",
  "tipo": "EMPRESA",
  "documento": "11.222.333/0001-81",
  "site": "https://www.cursoemvideo.com",
  "responsavel": "Nome de quem responde",
  "email": "contato@cursoemvideo.com"
}
```
- `tipo`: `EMPRESA` (CNPJ obrigatório), `ONG` ou `INDEPENDENTE` (CNPJ opcional).
- `documento`: com ou sem pontuação; o servidor confere os dígitos e guarda só os números.
- `site` opcional, precisa começar com `http://` ou `https://`.
- O `nome` é o crédito que as alunas veem no curso.
- Erros: 409 (e-mail, nome ou CNPJ já existem), 400 (CNPJ inválido, ou faltando em EMPRESA).
- A administradora **não define senha**. Reenviar o convite gera um código novo e o anterior morre; só 1 por minuto (429) e só antes do aceite (409).

**VerificacaoRequest** (os três `true` ou nada acontece: 400)
```json
{ "siteConferido": true, "dominioDoEmailConferido": true, "identificacaoConferida": true, "observacao": "opcional" }
```
Só funciona depois de a parceira aceitar o convite (senão 409).

**Parceira** (resposta)
```json
{
  "id": 3, "usuarioId": 41, "nome": "...", "tipo": "ONG", "documento": "11222333000181", "site": "...",
  "responsavel": "...", "email": "...", "situacao": "CONVIDADA", "criadaEm": "...",
  "aceitouOsTermos": true, "termosVersao": "2026-11-14", "termosAceitosEm": "...",
  "dominioDoEmailConfereComSite": true,
  "verificacao": { "siteConferido": false, "dominioDoEmailConferido": false, "identificacaoConferida": false,
                   "verificadaEm": null, "verificadaPor": null, "observacao": null },
  "motivoDaSuspensao": null,
  "cursos": 2
}
```
`dominioDoEmailConfereComSite` é só uma pista para a tela de verificação: `true` = o e-mail é do mesmo domínio do site, `false` = e-mail pessoal (gmail etc.) ou de outro domínio, `null` = não há site.

## 4. Telas da parceira

### Antes de ter conta (sem token)
- `GET /parceiras/termos` → `{ "versao": "2026-11-14", "titulo": "...", "texto": "..." }`. Mostre o texto inteiro antes do botão de aceitar.
- `POST /parceiras/convite/aceitar`
```json
{ "email": "contato@...", "codigo": "K7M2-9QXA", "senha": "mínimo 8", "aceitaTermos": true, "versaoTermos": "2026-11-14" }
```
  O código pode ser digitado com ou sem o hífen, em maiúscula ou minúscula. Vale 7 dias, 5 tentativas. Erro de código sempre responde a mesma mensagem ("Código inválido ou expirado.", 400). Se a `versaoTermos` não for a vigente, 400 pedindo para ler de novo. Sucesso devolve a `Parceira`; **depois disso é só fazer login normal**.

### Depois de logada (`/api/parceira`)

| Ação | Chamada |
|---|---|
| Meu perfil e situação | `GET /api/parceira/perfil` |
| Meus cursos | `GET /api/parceira/cursos?situacao=` |
| Detalhe (módulos e aulas) | `GET /api/parceira/cursos/{id}` |
| Criar curso | `POST /api/parceira/cursos` (mesmo corpo `CursoRequest` da gestão; nasce em RASCUNHO e o crédito é sempre o nome dela) |
| Editar dados | `PUT /api/parceira/cursos/{id}` |
| Enviar para revisão | `POST /api/parceira/cursos/{id}/revisao` |
| Módulos | `POST /api/parceira/cursos/{id}/modulos` · `PUT /api/parceira/cursos/{id}/modulos/ordem` · `PUT/DELETE /api/parceira/modulos/{id}` |
| Aulas | `POST /api/parceira/modulos/{id}/aulas` · `PUT /api/parceira/modulos/{id}/aulas/ordem` · `PUT/DELETE /api/parceira/aulas/{id}` |
| Métricas | `GET /api/parceira/cursos/{id}/metricas` |
| Histórico | `GET /api/parceira/cursos/{id}/historico` |

Os corpos e respostas de módulos e aulas são **os mesmos** da gestão da administradora (seção 5).
Curso de outra parceira (ou do TechMentor) responde **404**, como se não existisse.

## 5. Gestão de cursos (admin: `/api/admin`, parceira: `/api/parceira`)

Mesmas regras nos dois lados. Corpos:

- `CursoRequest`: `titulo`, `descricao`, `categoria`, `nivel`, `iconeUrl`, `instituicao` (a parceira não escolhe: é ignorado). Os campos `ativo` e `situacao` do corpo são ignorados na criação (sempre RASCUNHO).
- `ModuloGestaoRequest`: `{ "titulo": "..." }`. Entra no fim do curso.
- `AulaGestaoRequest`: `{ "titulo", "duracaoMinutos", "xpRecompensa" (opcional), "conteudoUrl" }`. Entra no fim do módulo.
  `conteudoUrl` aceita só YouTube (`https://www.youtube.com/watch?v=ID` com ID de 11 caracteres, ou `https://youtu.be/ID`) ou `.mp4` em https. Outra coisa: 400.
- Reordenar: `{ "ids": [12, 10, 11] }` com **todos** os itens, uma vez cada (400 se faltar ou sobrar).

**Situação do curso**: `RASCUNHO` → `EM_REVISAO` → `PUBLICADO` → `ARQUIVADO`. Só `PUBLICADO` aparece no catálogo das alunas; quem já estava matriculada num curso arquivado continua com acesso.

Regras que viram mensagem de erro (409 ou 400) e precisam de tela:
- Enviar para revisão / publicar exige **descrição**, **pelo menos um módulo** e **nenhum módulo vazio** (400 com o texto do que falta).
- **Aluna já estudou**: apagar aula/módulo com progresso → 409 ("2 alunas já estudaram esta aula..."); reordenar curso/módulo com progresso → 409. Editar texto, duração, XP e link continua liberado.
- **Curso publicado** não pode ficar sem módulos nem com módulo sem aulas (409).
- **Em revisão** a parceira não edita nada (409 "Este curso está em revisão..."). **Arquivado** também não (409).
- Curso com matrículas não volta para rascunho (409).

### Revisão (administradora)
A fila é a lista de cursos filtrada: `GET /api/admin/cursos?situacao=EM_REVISAO`.

| Decisão | Chamada | Efeito |
|---|---|---|
| Aprovar | `PATCH /api/admin/cursos/{id}/situacao` com `{ "situacao": "PUBLICADO" }` | publica; a parceira é avisada |
| Devolver | `POST /api/admin/cursos/{id}/devolucao` com `{ "comentario": "..." }` | volta a RASCUNHO; o comentário fica no curso |
| Recusar | `POST /api/admin/cursos/{id}/recusa` com `{ "comentario": "..." }` | vai para ARQUIVADO; o comentário fica no curso |

- O comentário é obrigatório (até 500 caracteres) e aparece no `CursoResponse` da parceira em `comentarioRevisao`. Some quando o curso é publicado.
- Devolver e recusar só valem para curso `EM_REVISAO` (409).
- Sair de `EM_REVISAO` para rascunho/arquivado pelo PATCH não é permitido (400): use devolver/recusar. `EM_REVISAO` não se escolhe pelo PATCH.
- Curso devolvido pode ser editado e **reenviado**. Curso recusado é definitivo: a parceira cria outro.

**CursoResponse** ganhou dois campos (só aparecem quando existem): `parceiraId` e `comentarioRevisao`. O campo `situacao` agora também pode ser `EM_REVISAO`.

## 6. Histórico (auditoria)

`GET /api/admin/cursos/{id}/historico`, `GET /api/parceira/cursos/{id}/historico`, `GET /api/admin/parceiras/{id}/historico`. Do mais recente para o mais antigo.

```json
{ "id": 9, "quando": "...", "autorId": 41, "autorNome": "Maria", "autorPapel": "PARCEIRA",
  "acao": "CURSO_ENVIADO_PARA_REVISAO", "descricao": "Curso enviado para revisão", "detalhe": null }
```
Mostre `descricao` (já em português) e `detalhe` quando houver (comentário da devolução, o que foi alterado...). `autorId`/`autorNome` vêm nulos quando foi o sistema.

## 7. Notificações com destino (sininho)

`GET /notificacoes/usuarios/{id}` agora traz, além dos campos antigos:

- `tipo`: categoria, para o ícone. `GERAL, CONQUISTA, NIVEL, CURSO, DESAFIO, DENUNCIA, MENTORIA, AVALIACAO, OPORTUNIDADE, PARCERIA, REVISAO`.
- `destino`: para onde o toque leva. `NENHUM` = só marca como lida.
- `referencia`: completa o destino (pode ser nula).

| `destino` | Leva para | `referencia` |
|---|---|---|
| `NENHUM` | só marca como lida | |
| `CONQUISTAS` | tela de conquistas | |
| `CURSO` | trilha do curso | título do curso |
| `DESAFIOS` | tela de desafios | |
| `MENTORIAS` | lista de mentorias | |
| `AVALIACOES` | avaliações | |
| `OPORTUNIDADES` | oportunidades/eventos | |
| `DENUNCIAS` | denúncias da pessoa | |
| `REVISAO_DE_CURSO` | admin: o curso na fila de revisão | id do curso |
| `PARCEIRA_PARA_VERIFICAR` | admin: a parceira que aceitou e espera a verificação | id da parceira |
| `CURSO_DA_PARCEIRA` | parceira: o curso (aprovado, devolvido ou recusado) | id do curso |
| `PERFIL_DA_PARCEIRA` | parceira: o próprio perfil (conta verificada ou suspensa) | |

Notificações antigas chegam como `GERAL` / `NENHUM`. Valor desconhecido de `tipo` ou `destino` deve cair no padrão (só marcar como lida), para o app não quebrar quando surgir um novo.

## 8. Termos de Parceria

O texto está em `backend/src/main/java/com/techmentor/api/service/TermosDeParceria.java`. **É um texto-base: antes de convidar parceiras reais, peça a revisão do jurídico.** Quando o texto mudar, suba a constante `VERSAO`.
