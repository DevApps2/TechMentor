# Testa o envio de e-mail (Gmail) ANTES de ligar a API.
# Lê as variáveis MAIL_* do seu rodar-local.ps1 e manda uma mensagem de teste.
# Uso:  .\testar-email.ps1 -Para "seu@email.com"

param(
    [Parameter(Mandatory = $true)][string]$Para
)

$ErrorActionPreference = 'Stop'
Set-Location $PSScriptRoot

$arquivo = Join-Path $PSScriptRoot 'rodar-local.ps1'
if (Test-Path $arquivo) {
    foreach ($linha in Get-Content $arquivo -Encoding UTF8) {
        if ($linha -match "^\s*\`$env:(\w+)\s*=\s*'(.*)'") {
            [Environment]::SetEnvironmentVariable($Matches[1], $Matches[2], 'Process')
        }
    }
}

function Falha($mensagem) {
    Write-Host "`nERRO: $mensagem" -ForegroundColor Red
    exit 1
}

foreach ($nome in 'MAIL_HOST', 'MAIL_PORT', 'MAIL_USER', 'MAIL_SENHA', 'MAIL_REMETENTE') {
    $valor = [Environment]::GetEnvironmentVariable($nome)
    if ([string]::IsNullOrWhiteSpace($valor) -or $valor -like '*COLE_AQUI*') { Falha "$nome não está preenchida no rodar-local.ps1." }
}
if ($env:MAIL_SENHA -match '\s') { Falha "A senha de app não pode ter espaços. Copie as 16 letras juntas." }

Write-Host "Enviando um e-mail de teste de $($env:MAIL_USER) para $Para ..." -ForegroundColor Cyan

try {
    $cliente = New-Object System.Net.Mail.SmtpClient($env:MAIL_HOST, [int]$env:MAIL_PORT)
    $cliente.EnableSsl = $true
    $cliente.Timeout = 15000
    $cliente.Credentials = New-Object System.Net.NetworkCredential($env:MAIL_USER, $env:MAIL_SENHA)

    $mensagem = New-Object System.Net.Mail.MailMessage($env:MAIL_REMETENTE, $Para)
    $mensagem.Subject = 'Teste de e-mail - TechMentor'
    $mensagem.Body = "Se você está lendo isto, o envio de e-mails da API do TechMentor está funcionando.`r`n`r`nEnviado em $(Get-Date -Format 'dd/MM/yyyy HH:mm')."
    $cliente.Send($mensagem)
} catch {
    $texto = $_.Exception.ToString()
    if ($texto -match '535|Authentication|autenticação|5\.7\.8') {
        Falha "O Gmail recusou o usuário ou a senha (535). Gere uma NOVA senha de app (myaccount.google.com/apppasswords), sem espaços, e confirme que a verificação em duas etapas está ligada."
    }
    if ($texto -match 'timed out|tempo|connect|conex|resolve|nome') {
        Falha "Não consegui conectar ao servidor de e-mail ($($env:MAIL_HOST):$($env:MAIL_PORT)). Quase sempre é a REDE bloqueando a porta 587. Teste em outra rede."
    }
    Falha "Falha ao enviar: $($_.Exception.Message)"
}

Write-Host "`nOK: e-mail enviado. Confira a caixa de entrada (e o spam) de $Para." -ForegroundColor Green
