param([string]$BaseUrl = 'http://127.0.0.1:8081')
$ErrorActionPreference = 'Stop'
$projectDirectory = Split-Path $PSScriptRoot -Parent
$accounts = Import-Clixml -LiteralPath (Join-Path $projectDirectory 'artifacts/private/access.xml')
$keys = @('COTIZADOR_TEST_EJECUTIVO_PASSWORD','COTIZADOR_TEST_APROBADOR_PASSWORD','COTIZADOR_TEST_URL')
$previous = @{}
foreach ($key in $keys) { $previous[$key] = [Environment]::GetEnvironmentVariable($key,'Process') }
try {
    $env:COTIZADOR_TEST_EJECUTIVO_PASSWORD = $accounts['ejecutivo'].GetNetworkCredential().Password
    $env:COTIZADOR_TEST_APROBADOR_PASSWORD = $accounts['aprobador'].GetNetworkCredential().Password
    $env:COTIZADOR_TEST_URL = $BaseUrl
    & python (Join-Path $PSScriptRoot 'test_frontend_roles.py')
    if ($LASTEXITCODE -ne 0) { throw 'Fallo en la prueba de roles; consulta la salida y artifacts/roles-review.' }
} finally {
    foreach ($key in $keys) { [Environment]::SetEnvironmentVariable($key,$previous[$key],'Process') }
}
