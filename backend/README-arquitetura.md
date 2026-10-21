# Organização do backend

A API usa uma estrutura simples por camada. Os arquivos ficam diretamente nas pastas existentes, sem criar uma subpasta para cada módulo.

```text
src/main/java/com/techmentor/api/
├── controller/   # endpoints HTTP
├── dto/          # objetos de entrada e saída
├── model/        # entidades JPA compartilhadas
├── repository/   # consultas e persistência
├── service/      # regras de negócio, separadas por responsabilidade
├── ApiException.java
└── ApiExceptionHandler.java
```

Os serviços do módulo do Matheus ficam juntos em `service/`: `MentoraService` e `AgendamentoService`, `OportunidadeService`, `DenunciaService` e `BloqueioService`. `UsuarioAtualService` resolve o usuário das requisições sem substituir o `UsuarioService` de autenticação existente; `RegraAcesso` concentra regras compartilhadas. Os DTOs foram separados por assunto dentro de `dto/`.

As rotas HTTP foram mantidas. As rotas protegidas ainda usam `X-Usuario-Id`; a integração com uma identidade autenticada confiável continua necessária antes da publicação.
