param([string]$Container = 'gastos_etl_mssql', [int]$Port = 8081, [switch]$SkipBuild)
$ErrorActionPreference = 'Stop'
$projectDirectory = Split-Path $PSScriptRoot -Parent
$privateDirectory = Join-Path $projectDirectory 'artifacts/private'
New-Item -ItemType Directory -Path $privateDirectory -Force | Out-Null
if (Get-NetTCPConnection -State Listen -LocalPort $Port -ErrorAction SilentlyContinue) {
    throw "El puerto $Port esta ocupado. Usa -Port con otro puerto o revisa el proceso existente."
}
$javaCommand = (Get-Command java -ErrorAction Stop).Source
$localJavaHome = Split-Path (Split-Path $javaCommand -Parent) -Parent
$savedJavaHome = $env:JAVA_HOME
try {
    $env:JAVA_HOME = $localJavaHome
    Push-Location $projectDirectory
    try {
        & ./database/Initialize-Cotizador.ps1 -Container $Container
        if (-not $SkipBuild) {
            & ./gradlew.bat bootJar
            if ($LASTEXITCODE -ne 0) { throw 'La compilacion fallo.' }
        }
    } finally { Pop-Location }
} finally { $env:JAVA_HOME = $savedJavaHome }
. (Join-Path $projectDirectory 'database/Provision-LocalApp.ps1')
$databaseAccount = Initialize-LocalDatabaseAccount -Container $Container
$accessPath = Join-Path $privateDirectory 'access.xml'
if (-not (Test-Path -LiteralPath $accessPath)) {
    $accounts = @{}
    foreach ($username in @('ejecutivo', 'aprobador')) {
        $randomBytes = New-Object byte[] 18
        $generator = [Security.Cryptography.RandomNumberGenerator]::Create()
        try { $generator.GetBytes($randomBytes) } finally { $generator.Dispose() }
        $secret = 'Um9!' + [Convert]::ToBase64String($randomBytes)
        $accounts[$username] = New-Object System.Management.Automation.PSCredential($username, (ConvertTo-SecureString $secret -AsPlainText -Force))
    }
    $accounts | Export-Clixml -LiteralPath $accessPath
}
$accounts = Import-Clixml -LiteralPath $accessPath
$variables = @{
    SPRING_PROFILES_ACTIVE = 'local'; PORT = [string]$Port
    DB_USERNAME = $databaseAccount.Username; DB_PASSWORD = $databaseAccount.Password
    BOOTSTRAP_EJECUTIVO_PASSWORD = $accounts['ejecutivo'].GetNetworkCredential().Password
    BOOTSTRAP_APROBADOR_PASSWORD = $accounts['aprobador'].GetNetworkCredential().Password
}
$previous = @{}
try {
    foreach ($key in $variables.Keys) {
        $previous[$key] = [Environment]::GetEnvironmentVariable($key, 'Process')
        [Environment]::SetEnvironmentVariable($key, $variables[$key], 'Process')
    }
    $jar = Join-Path $projectDirectory 'build/libs/NewCotizador-0.0.1-SNAPSHOT.jar'
    if (-not (Test-Path -LiteralPath $jar)) { throw 'No existe bootJar; ejecuta sin -SkipBuild.' }
    $process = Start-Process -FilePath $javaCommand -ArgumentList @('-Xms128m', '-Xmx512m', '-jar', ('"' + $jar + '"')) `
        -WorkingDirectory $projectDirectory -WindowStyle Hidden -PassThru `
        -RedirectStandardOutput (Join-Path $privateDirectory 'application.log') `
        -RedirectStandardError (Join-Path $privateDirectory 'application-error.log')
    $process.Id | Set-Content -LiteralPath (Join-Path $privateDirectory 'application.pid')
} finally {
    foreach ($key in $variables.Keys) { [Environment]::SetEnvironmentVariable($key, $previous[$key], 'Process') }
}
Write-Host "Proceso $($process.Id) iniciado en http://localhost:$Port. Revisa artifacts/private/application.log para confirmar el arranque."
Write-Host 'Para ver tus claves locales: ./scripts/Show-LocalAccess.ps1'
$ready = $false
$deadline = [DateTime]::UtcNow.AddSeconds(90)
while ([DateTime]::UtcNow -lt $deadline) {
    $process.Refresh()
    if ($process.HasExited) { throw 'La aplicacion termino durante el arranque. Revisa artifacts/private/application.log.' }
    try {
        $health = Invoke-RestMethod -Uri "http://localhost:$Port/actuator/health" -TimeoutSec 2
        $readiness = Invoke-RestMethod -Uri "http://localhost:$Port/actuator/health/readiness" -TimeoutSec 2
        if ($health.status -eq 'UP' -and $readiness.status -eq 'UP') { $ready = $true; break }
    } catch { }
    Start-Sleep -Seconds 1
}
if (-not $ready) { throw 'La aplicacion no confirmo salud UP en 90 segundos. Revisa los logs antes de iniciar otra instancia.' }
Write-Host 'Aplicacion lista: health UP.'
