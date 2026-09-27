# Dot-source this file; credentials returned by the function must never be logged.
$script:CotizadorDatabaseDirectory = $PSScriptRoot

function Initialize-LocalDatabaseAccount {
    [CmdletBinding()]
    param([string]$Container = 'gastos_etl_mssql')

    $ErrorActionPreference = 'Stop'
    if ($env:OS -ne 'Windows_NT') {
        throw 'El aprovisionador local requiere Windows y DPAPI. Provisione la cuenta SQL manualmente en otros sistemas.'
    }
    if ($Container -notmatch '^[A-Za-z0-9][A-Za-z0-9_.-]*$') {
        throw 'Nombre de contenedor invalido.'
    }
    $privateDirectory = Join-Path (Split-Path $script:CotizadorDatabaseDirectory -Parent) 'artifacts/private'
    $credentialPath = Join-Path $privateDirectory 'database-credential.xml'
    $hasCredential = Test-Path -LiteralPath $credentialPath -PathType Leaf
    if ($hasCredential) {
        try {
            $credential = Import-Clixml -LiteralPath $credentialPath
            if ($credential -isnot [System.Management.Automation.PSCredential] -or $credential.UserName -ne 'cotizador_app') {
                throw 'Invalid credential type.'
            }
            $password = $credential.GetNetworkCredential().Password
        } catch {
            throw 'No se puede descifrar artifacts/private/database-credential.xml. Use el mismo usuario de Windows que provisiono la cuenta.'
        }
    } else {
        $bytes = New-Object byte[] 24
        $generator = [System.Security.Cryptography.RandomNumberGenerator]::Create()
        try { $generator.GetBytes($bytes) } finally { $generator.Dispose() }
        $password = 'Nc9!' + [BitConverter]::ToString($bytes).Replace('-', '').ToLowerInvariant()
        $credential = New-Object System.Management.Automation.PSCredential('cotizador_app', (ConvertTo-SecureString $password -AsPlainText -Force))
    }
    # Only this locally generated alphabet is permitted in the SQL literal below.
    if ($password -notmatch '^Nc9![a-f0-9]{48}$') {
        throw 'Formato de credencial local inesperado. Restaure la credencial original; no se rotara un login existente.'
    }
    $allowExisting = if ($hasCredential) { '1' } else { '0' }
    $sql = @'
SET NOCOUNT ON;
SET XACT_ABORT ON;
USE master;
IF EXISTS (SELECT 1 FROM sys.server_principals WHERE name = N'cotizador_app')
BEGIN
    IF __ALLOW_EXISTING__ = 0
        THROW 51110, 'Login existente sin credencial local. Restaure el archivo DPAPI original.', 1;
    IF NOT EXISTS (SELECT 1 FROM sys.sql_logins WHERE name = N'cotizador_app'
        AND is_disabled = 0 AND PWDCOMPARE(N'__PASSWORD__', password_hash) = 1)
        THROW 51111, 'La credencial local no coincide con el login activo.', 1;
END
ELSE
    CREATE LOGIN cotizador_app WITH PASSWORD = '__PASSWORD__', CHECK_POLICY = ON, CHECK_EXPIRATION = OFF;
USE Cotizador;
IF SCHEMA_ID(N'Cotizador') IS NULL THROW 51112, 'Ejecute Initialize-Cotizador.ps1 primero.', 1;
IF USER_ID(N'cotizador_app') IS NULL
    CREATE USER cotizador_app FOR LOGIN cotizador_app;
IF NOT EXISTS (SELECT 1 FROM sys.database_principals WHERE name = N'cotizador_app' AND sid = SUSER_SID(N'cotizador_app'))
    THROW 51113, 'El usuario existente pertenece a otro login.', 1;
GRANT SELECT, INSERT, UPDATE ON SCHEMA::Cotizador TO cotizador_app;
EXECUTE AS USER = N'cotizador_app';
DECLARE @valid BIT = CASE WHEN HAS_PERMS_BY_NAME(N'Cotizador', N'SCHEMA', N'SELECT') = 1
    AND HAS_PERMS_BY_NAME(N'Cotizador', N'SCHEMA', N'INSERT') = 1
    AND HAS_PERMS_BY_NAME(N'Cotizador', N'SCHEMA', N'UPDATE') = 1
    AND HAS_PERMS_BY_NAME(N'Cotizador', N'SCHEMA', N'DELETE') = 0
    AND HAS_PERMS_BY_NAME(N'Cotizador', N'SCHEMA', N'ALTER') = 0
    AND HAS_PERMS_BY_NAME(DB_NAME(), N'DATABASE', N'CREATE TABLE') = 0 THEN 1 ELSE 0 END;
REVERT;
IF @valid = 0 THROW 51114, 'Revisar permisos de la cuenta; no debe tener DELETE ni DDL.', 1;
GO
'@
    $sql = $sql.Replace('__ALLOW_EXISTING__', $allowExisting).Replace('__PASSWORD__', $password)
    $command = @'
set -eu
export SQLCMDPASSWORD="${MSSQL_SA_PASSWORD:-${SA_PASSWORD:-}}"
if [ -z "$SQLCMDPASSWORD" ]; then exit 20; fi
sqlcmd=/opt/mssql-tools18/bin/sqlcmd
if [ ! -x "$sqlcmd" ]; then sqlcmd=/opt/mssql-tools/bin/sqlcmd; fi
"$sqlcmd" -S localhost -U sa -C -b -r1 > /dev/null 2>&1 <<'COTIZADOR_SQL_INPUT'
__SQL__
COTIZADOR_SQL_INPUT
# Absorb CRLF appended by Windows PowerShell.
'@
    $command = $command.Replace('__SQL__', $sql).Replace("`r", '')
    # Secret travels through stdin only; neither argv nor temporary plaintext files.
    $command | & podman exec -i $Container sh -s
    if ($LASTEXITCODE -ne 0) {
        throw 'No se pudo provisionar cotizador_app. Verifique contenedor, clave SA y esquema. Si el login ya existe, restaure artifacts/private/database-credential.xml del usuario Windows original; no se cambia su clave. Revise tambien que no tenga permisos DELETE/DDL ni un usuario asociado a otro login.'
    }
    if (-not $hasCredential) {
        New-Item -ItemType Directory -Path $privateDirectory -Force | Out-Null
        $credential | Export-Clixml -LiteralPath $credentialPath
    }
    return [PSCustomObject]@{ Username = 'cotizador_app'; Password = $password }
}
