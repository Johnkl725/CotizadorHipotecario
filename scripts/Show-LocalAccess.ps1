# Ejecutar en una terminal privada: muestra las credenciales locales al propietario.
$ErrorActionPreference = 'Stop'
$accessPath = Join-Path (Split-Path $PSScriptRoot -Parent) 'artifacts/private/access.xml'
if (-not (Test-Path -LiteralPath $accessPath)) { throw 'Primero ejecuta scripts/Start-Local.ps1.' }
$accounts = Import-Clixml -LiteralPath $accessPath
foreach ($username in @('ejecutivo', 'aprobador')) {
    Write-Host ($username + ': ' + $accounts[$username].GetNetworkCredential().Password)
}
