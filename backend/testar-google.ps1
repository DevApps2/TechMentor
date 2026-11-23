# Testa as credenciais do Google Calendar / Meet ANTES de ligar a API.
# Lê as variáveis GOOGLE_* do seu rodar-local.ps1 (não precisa ligar nada) e faz o mesmo que a API faz:
#   1) troca o refresh token por um token de acesso;
#   2) lê a agenda;
#   3) cria um evento de teste COM sala do Meet e apaga em seguida.
# Uso:  .\testar-google.ps1

$ErrorActionPreference = 'Stop'
Set-Location $PSScriptRoot

function Carregar-Variaveis {
    $arquivo = Join-Path $PSScriptRoot 'rodar-local.ps1'
    if (Test-Path $arquivo) {
        foreach ($linha in Get-Content $arquivo -Encoding UTF8) {
            if ($linha -match "^\s*\`$env:(\w+)\s*=\s*'(.*)'") {
                [Environment]::SetEnvironmentVariable($Matches[1], $Matches[2], 'Process')
            }
        }
    }
}

function Falha($mensagem) {
    Write-Host "`nERRO: $mensagem" -ForegroundColor Red
    exit 1
}

# Tenta ler o JSON de erro que o Google devolveu
function Corpo-Do-Erro($erro) {
    try {
        if ($erro.ErrorDetails -and $erro.ErrorDetails.Message) { return ($erro.ErrorDetails.Message | ConvertFrom-Json) }
    } catch { }
    return $null
}

function Explicar($erro) {
    $corpo = Corpo-Do-Erro $erro
    $codigo = if ($corpo -and $corpo.error -is [string]) { $corpo.error } elseif ($corpo -and $corpo.error.status) { $corpo.error.status } else { '' }
    $detalhe = if ($corpo -and $corpo.error_description) { $corpo.error_description } elseif ($corpo -and $corpo.error.message) { $corpo.error.message } else { $erro.Exception.Message }

    switch -Regex ("$codigo $detalhe") {
        'invalid_grant'       { return "O refresh token venceu ou foi revogado (invalid_grant). Gere outro (guia, parte 6.2-D). Se o app do Google ficou em 'Teste', publique-o (6.2-B): em teste o token morre em 7 dias." }
        'invalid_client|unauthorized_client' { return "GOOGLE_CLIENT_ID ou GOOGLE_CLIENT_SECRET errados (ou com espaço no fim). Copie de novo da tela de credenciais." }
        'accessNotConfigured|has not been used|SERVICE_DISABLED' { return "A Google Calendar API não está ativada no projeto das credenciais (guia, parte 6.2-A, passo 3)." }
        'insufficient|PERMISSION_DENIED|forbidden' { return "O token não tem permissão para criar eventos. Gere de novo com o escopo https://www.googleapis.com/auth/calendar.events (ou .../calendar). Detalhe: $detalhe" }
        default {
            if (-not $corpo) { return "Não consegui falar com o Google. Quase sempre é a REDE deste computador (filtro de internet de empresa/escola, proxy ou firewall). Teste em outra rede ou no hotspot do celular. Detalhe: $detalhe" }
            return "O Google respondeu: $codigo $detalhe"
        }
    }
}

Carregar-Variaveis

foreach ($nome in 'GOOGLE_CLIENT_ID', 'GOOGLE_CLIENT_SECRET', 'GOOGLE_REFRESH_TOKEN') {
    $valor = [Environment]::GetEnvironmentVariable($nome)
    if ([string]::IsNullOrWhiteSpace($valor) -or $valor -like '*COLE_AQUI*') { Falha "$nome não está preenchida no rodar-local.ps1." }
    if ($valor -ne $valor.Trim()) { Falha "$nome tem espaço no começo ou no fim. Apague o espaço." }
}

# 1) token de acesso
Write-Host "1/3 Pedindo um token de acesso ao Google..." -ForegroundColor Cyan
try {
    $token = Invoke-RestMethod -Method Post -Uri 'https://oauth2.googleapis.com/token' -TimeoutSec 20 -Body @{
        client_id     = $env:GOOGLE_CLIENT_ID
        client_secret = $env:GOOGLE_CLIENT_SECRET
        refresh_token = $env:GOOGLE_REFRESH_TOKEN
        grant_type    = 'refresh_token'
    }
} catch { Falha (Explicar $_) }
Write-Host "    OK: o Google aceitou as credenciais." -ForegroundColor Green
$cabecalho = @{ Authorization = "Bearer $($token.access_token)" }

# 2) ler a agenda
Write-Host "2/3 Lendo a agenda da conta..." -ForegroundColor Cyan
try {
    Invoke-RestMethod -Uri 'https://www.googleapis.com/calendar/v3/calendars/primary/events?maxResults=1' -Headers $cabecalho -TimeoutSec 20 | Out-Null
} catch { Falha (Explicar $_) }
Write-Host "    OK: a agenda respondeu." -ForegroundColor Green

# 3) criar um evento de teste com Meet e apagar
Write-Host "3/3 Criando um evento de teste com sala do Meet (e apagando em seguida)..." -ForegroundColor Cyan
$inicio = (Get-Date).AddDays(30).Date.AddHours(3).ToUniversalTime()
$evento = @{
    summary        = 'TESTE TechMentor (pode apagar)'
    start          = @{ dateTime = $inicio.ToString("yyyy-MM-ddTHH:mm:ssZ") }
    end            = @{ dateTime = $inicio.AddMinutes(15).ToString("yyyy-MM-ddTHH:mm:ssZ") }
    transparency   = 'transparent'
    conferenceData = @{ createRequest = @{ requestId = [guid]::NewGuid().ToString(); conferenceSolutionKey = @{ type = 'hangoutsMeet' } } }
} | ConvertTo-Json -Depth 6

try {
    $criado = Invoke-RestMethod -Method Post -TimeoutSec 20 -Headers $cabecalho -ContentType 'application/json' -Body $evento `
        -Uri 'https://www.googleapis.com/calendar/v3/calendars/primary/events?conferenceDataVersion=1&sendUpdates=none'
} catch { Falha (Explicar $_) }

$link = $criado.hangoutLink
try {
    Invoke-RestMethod -Method Delete -Headers $cabecalho -TimeoutSec 20 -Uri "https://www.googleapis.com/calendar/v3/calendars/primary/events/$($criado.id)?sendUpdates=none" | Out-Null
} catch { Write-Host "    (aviso: não consegui apagar o evento de teste; apague 'TESTE TechMentor' na agenda)" -ForegroundColor Yellow }

if ([string]::IsNullOrWhiteSpace($link)) {
    Falha "O Google criou o evento, mas NÃO devolveu o link do Meet. Confirme que a conta pode criar reuniões do Meet."
}
Write-Host "    OK: sala criada ($link) e evento de teste apagado." -ForegroundColor Green

Write-Host "`nOK: o Google aceitou as credenciais e a agenda respondeu. A sala do Meet vai funcionar." -ForegroundColor Green
