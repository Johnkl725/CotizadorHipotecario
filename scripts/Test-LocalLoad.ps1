param([int]$Users = 60, [int]$Iterations = 20, [switch]$Persist, [string]$BaseUrl = 'http://localhost:8081')
$ErrorActionPreference = 'Stop'
$projectDirectory = Split-Path $PSScriptRoot -Parent
$accounts = Import-Clixml -LiteralPath (Join-Path $projectDirectory 'artifacts/private/access.xml')
$oldExecutive = $env:LOAD_EXECUTIVE_PASSWORD
$oldApprover = $env:LOAD_APPROVER_PASSWORD
try {
    $env:LOAD_EXECUTIVE_PASSWORD = $accounts['ejecutivo'].GetNetworkCredential().Password
    $env:LOAD_APPROVER_PASSWORD = $accounts['aprobador'].GetNetworkCredential().Password
    $arguments = @((Join-Path $PSScriptRoot 'load_test.py'), '--base', $BaseUrl, '--users', $Users, '--iterations', $Iterations,
        '--output', (Join-Path $projectDirectory ('artifacts/load-' + $(if ($Persist) {'mixed'} else {'simulation'}) + '.json')))
    if ($Persist) { $arguments += '--persist' }
    & python @arguments
    if ($LASTEXITCODE -ne 0) { throw 'La prueba de carga fallo; revisa el reporte.' }
} finally {
    $env:LOAD_EXECUTIVE_PASSWORD = $oldExecutive
    $env:LOAD_APPROVER_PASSWORD = $oldApprover
}
