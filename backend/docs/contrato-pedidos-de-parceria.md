# Contrato da API: pedidos de parceria (site "Seja nosso parceiro")

Para quem faz o site (formulário) e a tela da administradora no app. Complementa o `contrato-parceiras-e-cursos.md`.

- Base: `/techmentor/api`. Erros no formato de sempre: `{ "erro", "codigo", "campos" }`.
- Antes de existir conta, a instituição **pede**; a administradora **aprova ou recusa**. Aprovar cria a conta de parceira e manda o convite que já existia (código + link para o portal).

```
SITE: pedido  →  fila da admin (PENDENTE)  →  aprovar  →  e-mail de convite  →  portal: cria a senha  →  verificação  →  cursos
                                            └→ recusar (com motivo)  →  e-mail de resposta
```

## 1. O formulário do site (rota pública, sem login)

`POST /parceiras/pedidos`

```json
{
  "nome": "Instituto Aprender",
  "tipo": "ONG",
  "documento": "11.222.333/0001-81",
  "site": "https://www.institutoaprender.org.br",
  "responsavel": "Marina Duarte",
  "email": "marina@institutoaprender.org.br",
  "mensagem": "Temos doze cursos de lógica e web em vídeo, gratuitos, no YouTube.",
  "aceitaPrivacidade": true,
  "versaoPolitica": "2026-11-18",
  "website": ""
}
```

| Campo | Regra |
|---|---|
| `nome` | obrigatório, até 120. É o crédito que as alunas veem nos cursos |
| `tipo` | `EMPRESA`, `ONG` ou `INDEPENDENTE`. **EMPRESA exige CNPJ** |
| `documento` | opcional (CNPJ, com ou sem pontuação). O servidor confere os dígitos: CNPJ inventado → 400 `CNPJ inválido.` |
| `site` | opcional, precisa começar com `http://` ou `https://` |
| `responsavel` | obrigatório, até 120 |
| `email` | obrigatório, válido. **O convite vai para ele**, se o pedido for aprovado |
| `mensagem` | obrigatória, **de 20 a 1000 caracteres**: o que pretende oferecer |
| `aceitaPrivacidade` | precisa ser `true` |
| `versaoPolitica` | a versão do texto da política de privacidade que estava na tela; **fica guardada como prova do consentimento** |
| `website` | **ARMADILHA CONTRA ROBÔS.** Campo escondido (CSS `display:none`, `tabindex="-1"`, `autocomplete="off"`). Pessoas não preenchem. Mande vazio ou omita |

**Respostas**
- `201` `{ "mensagem": "Recebemos o seu pedido. Nossa equipe vai analisar e responder por e-mail." }`
- `400` dado inválido. `campos` traz o erro de cada campo (mostre embaixo do input) e `erro` traz o resumo.
- `429` o mesmo endereço mandou pedidos demais (padrão: **5 por hora**). Mostre a mensagem da API.

**Importante para a tela do site**
- A resposta `201` é **sempre a mesma**, exista ou não o e-mail no sistema, seja pedido repetido ou de robô. Isso é de propósito: a rota não pode servir para descobrir quem já é parceira. Então a tela de sucesso deve dizer algo como "Recebemos! Se o pedido for aprovado, você recebe um e-mail" e **não** prometer que algo foi criado.
- A instituição recebe um e-mail de confirmação; a equipe (`techmentor.apps@gmail.com`) recebe o aviso do pedido.
- CORS: o endereço do site precisa estar em `CORS_ORIGENS` no servidor (em desenvolvimento, `http://localhost:5173` já vale).
- O site **precisa ter a página da política de privacidade** com a versão que é enviada em `versaoPolitica`. O texto é de responsabilidade de vocês (revisar com o jurídico).

## 2. A fila da administradora (`/api/admin/pedidos-parceria`, só ADMIN)

| Ação | Chamada | Resposta |
|---|---|---|
| Listar | `GET /api/admin/pedidos-parceria?situacao=PENDENTE` | lista de `Pedido`, do mais novo para o mais antigo (filtro opcional: `PENDENTE`, `APROVADO`, `RECUSADO`) |
| Detalhar | `GET /api/admin/pedidos-parceria/{id}` | `Pedido` |
| Aprovar | `POST /api/admin/pedidos-parceria/{id}/aprovacao` | `Pedido` (APROVADO, com `parceiraId`) |
| Recusar | `POST /api/admin/pedidos-parceria/{id}/recusa` com `{ "motivo": "..." }` | `Pedido` (RECUSADO) |

**Pedido**
```json
{
  "id": 7, "nome": "Instituto Aprender", "tipo": "ONG", "documento": "11222333000181",
  "site": "https://www.institutoaprender.org.br", "responsavel": "Marina Duarte",
  "email": "marina@institutoaprender.org.br",
  "mensagem": "Temos doze cursos...",
  "situacao": "PENDENTE", "criadoEm": "2026-11-18T14:00:00-03:00",
  "dominioDoEmailConfereComSite": true,
  "consentimentoEm": "2026-11-18T14:00:00-03:00", "politicaVersao": "2026-11-18",
  "decididoEm": null, "decididoPor": null, "motivoDaRecusa": null, "parceiraId": null
}
```

- `dominioDoEmailConfereComSite` é uma **pista** para a análise: `true` = o e-mail é do mesmo domínio do site, `false` = e-mail pessoal (gmail etc.) ou de outro domínio, `null` = sem site.
- **Aprovar** faz o mesmo que "Convidar" em *Gerenciar parceiras*: cria a conta (inativa) e envia o e-mail com o código e o botão "Criar minha senha". Se o e-mail, o nome ou o CNPJ já existem, responde `409` com a mensagem e o pedido **continua pendente**. Depois de aprovar, a parceira aparece em *Gerenciar parceiras* como `CONVIDADA`, onde segue o fluxo de verificação de sempre.
- **Recusar** exige o motivo (até 500 caracteres) e o envia por e-mail à instituição.
- Pedido já respondido: `409` "Este pedido já foi respondido."

## 3. Aviso no sininho da administradora
Cada pedido novo gera uma notificação para **todas as administradoras** com:
- `tipo`: `PARCERIA`
- `destino`: `PEDIDO_DE_PARCERIA`
- `referencia`: o **id do pedido** (leva à tela do pedido)

## 4. O que a tela da administradora no app precisa
Uma lista "Pedidos de parceria" (com contador de pendentes), o detalhe do pedido (mostrando a mensagem, o site, o CNPJ e a pista do domínio) e os botões **Aprovar** e **Recusar** (este com o campo do motivo).
