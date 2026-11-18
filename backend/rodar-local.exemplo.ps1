# MODELO. Copie para "rodar-local.ps1" (que o Git ignora), preencha os COLE_AQUI e rode:  .\rodar-local.ps1
# NUNCA coloque valores reais neste arquivo de modelo, e nunca commite o rodar-local.ps1.
# O passo a passo completo está em backend/docs/rodar-no-notebook.md

$ErrorActionPreference = 'Stop'
Set-Location $PSScriptRoot

# ---------------------------------------------------------------- banco
# mysql = dados que sobrevivem a reinícios (recomendado). h2 = banco em memória, zera a cada reinício (só demonstração).
$env:SPRING_PROFILES_ACTIVE      = 'mysql'
$env:SPRING_DATASOURCE_USERNAME  = 'techmentor'
$env:SPRING_DATASOURCE_PASSWORD  = 'COLE_AQUI_A_SENHA_DO_USUARIO_techmentor_DO_MYSQL'

# ---------------------------------------------------------------- segurança
# Gere com:  $b = New-Object byte[] 48; [System.Security.Cryptography.RandomNumberGenerator]::Create().GetBytes($b); [Convert]::ToBase64String($b)
$env:JWT_SECRET = 'COLE_AQUI_A_CHAVE_GERADA'

# ---------------------------------------------------------------- e-mail (Gmail com senha de app)
$env:MAIL_HOST      = 'smtp.gmail.com'
$env:MAIL_PORT      = '587'
$env:MAIL_USER      = 'techmentor.apps@gmail.com'
$env:MAIL_SENHA     = 'COLE_AQUI_A_SENHA_DE_APP_DE_16_LETRAS'
$env:MAIL_REMETENTE = 'techmentor.apps@gmail.com'

# Quem recebe o aviso de pedido de parceria e de denúncia
$env:PEDIDOS_EMAIL   = 'techmentor.apps@gmail.com'
$env:MODERACAO_EMAIL = 'techmentor.apps@gmail.com'

# ---------------------------------------------------------------- Google Calendar / Meet
$env:GOOGLE_CLIENT_ID     = 'COLE_AQUI'
$env:GOOGLE_CLIENT_SECRET = 'COLE_AQUI'
$env:GOOGLE_REFRESH_TOKEN = 'COLE_AQUI'
# true = o Google convida a aluna e a mentora por e-mail. Use false só em testes com e-mails de mentira.
$env:GOOGLE_CONVIDAR = 'true'

# ---------------------------------------------------------------- sites que podem chamar a API pelo navegador
# SÓ origens (esquema + domínio), SEM caminho e sem barra no fim. Separe por vírgula.
$env:CORS_ORIGENS = 'https://devapps2.github.io,https://COLE_AQUI_O_PORTAL.vercel.app,http://localhost:5173'
# Endereço do portal das parceiras: é o link do botão "Criar minha senha" no e-mail de convite.
$env:PORTAL_URL = 'https://COLE_AQUI_O_PORTAL.vercel.app'

# ---------------------------------------------------------------- atrás do ngrok
$env:PROXY_CONFIAVEL = 'true'
# A API fica exposta na internet: sem documentação pública (Swagger).
$env:SPRINGDOC_SWAGGER_UI_ENABLED = 'false'
$env:SPRINGDOC_API_DOCS_ENABLED   = 'false'

# ---------------------------------------------------------------- confere se faltou preencher alguma coisa
$faltando = Get-ChildItem Env: | Where-Object { $_.Value -like '*COLE_AQUI*' } | ForEach-Object { $_.Name }
if ($faltando) {
    Write-Host "`nFALTA PREENCHER no rodar-local.ps1: $($faltando -join ', ')" -ForegroundColor Red
    exit 1
}

Write-Host "`nIniciando a API do TechMentor (perfil: $($env:SPRING_PROFILES_ACTIVE))..." -ForegroundColor Cyan
.\mvnw.cmd spring-boot:run
