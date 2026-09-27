$ErrorActionPreference = 'Stop'
$projectDirectory = Split-Path $PSScriptRoot -Parent
$accounts = Import-Clixml -LiteralPath (Join-Path $projectDirectory 'artifacts/private/access.xml')
$oldExecutive = $env:LOAD_EXECUTIVE_PASSWORD
$oldApprover = $env:LOAD_APPROVER_PASSWORD
try {
    $env:LOAD_EXECUTIVE_PASSWORD = $accounts['ejecutivo'].GetNetworkCredential().Password
    $env:LOAD_APPROVER_PASSWORD = $accounts['aprobador'].GetNetworkCredential().Password
    Push-Location $projectDirectory
    try {
        & python ./scripts/browser_smoke.py
        if ($LASTEXITCODE -ne 0) { throw 'Fallo la prueba de navegador.' }
    } finally { Pop-Location }
} finally {
    $env:LOAD_EXECUTIVE_PASSWORD = $oldExecutive
    $env:LOAD_APPROVER_PASSWORD = $oldApprover
}
