$ErrorActionPreference = 'Stop'
$projectDirectory = Split-Path $PSScriptRoot -Parent
$pidFile = Join-Path $projectDirectory 'artifacts/private/application.pid'
if (-not (Test-Path -LiteralPath $pidFile)) { throw 'No hay PID local registrado.' }
$applicationPid = [int](Get-Content -LiteralPath $pidFile)
$process = Get-CimInstance Win32_Process -Filter "ProcessId = $applicationPid"
if (-not $process) { Write-Host 'El proceso local ya termino.'; return }
$expectedJar = Join-Path $projectDirectory 'build/libs/NewCotizador-0.0.1-SNAPSHOT.jar'
if ($process.Name -ne 'java.exe' -or -not $process.CommandLine.Contains($expectedJar)) {
    throw 'El PID pertenece a otro proceso; no se detendra.'
}
Stop-Process -Id $applicationPid
Write-Host 'Aplicacion local detenida. SQL Server permanece activo.'
