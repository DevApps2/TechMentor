# Organização do backend

A API usa uma estrutura simples por camada. Os arquivos ficam diretamente nas pastas existentes, sem criar uma subpasta para cada módulo.

```text
src/main/java/com/techmentor/api/
├── controller/   # endpoints HTTP
├── dto/          # objetos de entrada e saída
├── model/        # entidades JPA compartilhadas
├── repository/   # consultas e persistência
├── service/      # regras de negócio, separadas por responsabilidade
├── security/     # JWT: geração do token e filtro que identifica quem chama
├── config/       # SecurityConfig e outras configurações
└── exceptions/   # ApiException e o tratamento dos erros ({"erro": "mensagem"})
```

Os serviços do módulo do Matheus ficam juntos em `service/`: `MentoraService` e `AgendamentoService`, `OportunidadeService`, `DenunciaService` e `BloqueioService`. `UsuarioAtualService` resolve o usuário das requisições sem substituir o `UsuarioService` de autenticação existente; `RegraAcesso` concentra regras compartilhadas. Os DTOs foram separados por assunto dentro de `dto/`.

A identidade de quem chama vem do token JWT (`security/`), e `UsuarioAtualService.buscarAtual()` entrega o usuário logado aos controllers; nenhuma rota aceita um id de usuário enviado pelo cliente. As regras por URL (administradora, dona do dado) ficam em `config/SecurityConfig`, e as regras que dependem do dado ficam nos serviços, com `RegraAcesso`.
