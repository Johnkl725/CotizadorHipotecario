param(
    [string]$Container = 'gastos_etl_mssql',
    [switch]$IncludeDemoData
)
$ErrorActionPreference = 'Stop'
$scripts = @('01-create-database.sql', '02-schema.sql', '05-app-evolution.sql')
if ($IncludeDemoData) { $scripts += '03-demo-clientes.sql' }
$scripts += '04-smoke-test.sql'
foreach ($script in $scripts) {
    $source = Join-Path $PSScriptRoot $script
    $destination = '/tmp/newcotizador-' + [guid]::NewGuid().ToString('N') + '.sql'
    & podman cp $source "${Container}:$destination"
    if ($LASTEXITCODE -ne 0) { throw "No se pudo copiar $script." }
    # La clave permanece dentro del contenedor y no aparece en argumentos ni logs.
    $command = @'
set -eu
export SQLCMDPASSWORD="${MSSQL_SA_PASSWORD:-${SA_PASSWORD:-}}"
if [ -z "$SQLCMDPASSWORD" ]; then
    echo 'No hay clave SA en el entorno del contenedor. Usa sqlcmd interactivo segun README.' >&2
    exit 1
fi
sqlcmd=/opt/mssql-tools18/bin/sqlcmd
if [ ! -x "$sqlcmd" ]; then sqlcmd=/opt/mssql-tools/bin/sqlcmd; fi
trap 'rm -f "$1"' EXIT
"$sqlcmd" -S localhost -U sa -C -b -r1 -i "$1"
# Fin: absorbe el CRLF que Windows PowerShell agrega al pipe.
'@
    $command.Replace("`r", '') | & podman exec -i $Container sh -s -- $destination
    if ($LASTEXITCODE -ne 0) { throw "Fallo al ejecutar $script; no se continuara." }
    Write-Host "OK: $script"
}
