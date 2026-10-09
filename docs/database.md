# Persistencia SQL Server

Esquema: Cotizador. Tablas: empleado, cliente, producto_hipotecario, solicitud_credito, solicitud_cliente, inmueble_garantia y solicitud_historial_estado.

## Migración

database/Initialize-Cotizador.ps1 ejecuta 01-create-database.sql y V2_schema.sql. IncludeDemoData añade 03-seed-data.sql. Las tablas y filas existentes se conservan. La ejecución usa sqlcmd -b, XACT_ABORT y una transacción para V2.

V2 corrige las FK heredadas que apuntaban accidentalmente a dbo: comprueba primero que las referencias existan en Cotizador; ante una referencia huérfana aborta y solicita conciliación, sin borrar filas.

Las nuevas columnas tasa_aplicada e ingreso_evaluado conservan los valores financieros del registro. Para solicitudes antiguas sin snapshot, la migración utiliza los valores actuales del catálogo; no reconstruye valores históricos que no se guardaron.

Índices de ejecutivo/fecha y estado/fecha apoyan las carteras. @Version mapea BIGINT. Las columnas CHAR(3) de documento y moneda se declaran como CHAR en JPA para validar contra SQL Server.

## Cuenta de aplicación

Provision-LocalApp.ps1 crea/verifica cotizador_app con SELECT, INSERT y UPDATE sobre el esquema. No otorga DELETE ni DDL. Las mutaciones del agregado agregan historial sin borrar hijos. Migraciones con cuenta administrativa separada.

Las claves locales están cifradas con DPAPI en artifacts/private, fuera de Git. El aprovisionador no rota ni sustituye silenciosamente cuentas existentes.

## Datos de demostración

IncludeDemoData inserta tres productos y dos clientes sintéticos por nombre/documento si no existen. No modifica ingresos ni tasas existentes. Start-Local.ps1 activa esos datos en el entorno local.

Las cuentas ejecutivo/aprobador se crean desde Spring bajo el perfil local, con contraseñas aleatorias guardadas por el script de arranque y hashes BCrypt. No hay cuentas con claves predeterminadas.
