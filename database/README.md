# Paso 1 — @DatabaseAgent

Instancia detectada: `gastos_etl_mssql`, SQL Server 2022, puerto del host **14330** hacia el puerto 1433 del contenedor. No hace falta crear otro contenedor.

Desde la raiz del proyecto, en PowerShell:

```powershell
podman ps
./database/Initialize-Cotizador.ps1
# Opcional: agrega cuatro clientes ficticios para desarrollo.
./database/Initialize-Cotizador.ps1 -IncludeDemoData
```

El inicializador utiliza la clave SA que ya existe dentro del entorno del contenedor, sin mostrarla ni guardarla en el repositorio. Si esa clave ha cambiado o no esta disponible, ejecutar manualmente los scripts con el procedimiento siguiente. Cada error SQL detiene la ejecucion (`sqlcmd -b`).

## Alternativa manual con clave interactiva

```powershell
podman cp ./database gastos_etl_mssql:/tmp/newcotizador-database
podman exec -it gastos_etl_mssql /opt/mssql-tools18/bin/sqlcmd -S localhost -U sa -C -b -i /tmp/newcotizador-database/01-create-database.sql
podman exec -it gastos_etl_mssql /opt/mssql-tools18/bin/sqlcmd -S localhost -U sa -C -b -i /tmp/newcotizador-database/02-schema.sql
podman exec -it gastos_etl_mssql /opt/mssql-tools18/bin/sqlcmd -S localhost -U sa -C -b -i /tmp/newcotizador-database/05-app-evolution.sql
podman exec -it gastos_etl_mssql /opt/mssql-tools18/bin/sqlcmd -S localhost -U sa -C -b -i /tmp/newcotizador-database/04-smoke-test.sql
```

sqlcmd solicita la clave. En imagenes antiguas la ruta puede ser `/opt/mssql-tools/bin/sqlcmd`. `-C` acepta el certificado local autofirmado; la configuracion de certificados de produccion debe ser diferente.

## Resultado y alcance

- `01`: crea la base `Cotizador` si falta.
- `02`: crea el schema `Cotizador` y las tres tablas del diseno, con sus tipos, nulabilidad y valores predeterminados originales. Agrega indices para la bandeja por estado/fecha y las claves foraneas.
- `03`: inserta clientes ficticios opcionales sin sobrescribir DNIs existentes. Los scores son ejemplos, no equivalencias oficiales entre centrales.
- `05`: se ejecuta despues de `02`; agrega snapshots de riesgo, auditoria de decision, version optimista e indices de paginacion. Detalles y permisos minimos en [docs/database.md](../docs/database.md).
- `04`: comprueba tablas e insercion relacionada, decimales y valores predeterminados. Revierte los registros de prueba; SQL Server puede consumir valores IDENTITY incluso con rollback.

Los scripts no borran tablas ni bases. Pueden repetirse, pero no migran estructuras preexistentes incompatibles. El DDL se ejecuta en una transaccion; la creacion de la base se ejecuta separadamente. No ejecutar inicializadores simultaneos. No se crean usuarios con claves conocidas ni cotizaciones persistentes.

## Conexion desde Spring Boot — @BackendAgent

Se agrego el driver `com.microsoft.sqlserver:mssql-jdbc`, cuya version administra Spring Boot. `application-local.properties` prepara la conexion para el Paso 2, cuando se incorpore Data JPA:

```text
SPRING_PROFILES_ACTIVE=local
DB_USERNAME=<usuario SQL>
DB_PASSWORD=<clave local>
DB_URL=jdbc:sqlserver://localhost:14330;databaseName=Cotizador;encrypt=true;trustServerCertificate=true;
```

Configurar estas variables en el entorno o en la ejecucion del IDE; no versionar claves. La aplicacion debera usar una cuenta propia con permisos minimos. `ddl-auto=validate` evita que Hibernate modifique el esquema; el dialecto se detectara por JDBC.

## Decisiones para el Paso 2 — @ArchitectAgent

Antes de las clases: `controller -> service -> repository -> entity`, DTOs para entrada/salida y `exception` para errores uniformes. Seguridad por roles y propiedad de cotizaciones, calculo con BigDecimal y aprobacion transaccional que impida decisiones simultaneas.

El diseno aun no define umbrales de score/DSTI, tasa base, deudas mensuales existentes, tipo de tasa persistido ni auditoria de aprobaciones. No se inventaron estas reglas en SQL; requeriran configuracion explicita y, donde corresponda, una migracion. Las TEA se guardan como porcentaje (8.50 significa 8.50%); el servicio debera convertirlas a fraccion al calcular la TEM.

Referencia: [Microsoft: contenedores SQL Server y sqlcmd](https://learn.microsoft.com/en-us/sql/linux/quickstart-install-connect-docker).
