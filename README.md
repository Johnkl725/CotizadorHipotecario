# UMBRAL · Cotizador hipotecario

Aplicación de créditos en soles con **Angular 18, Spring Boot, Java 17 y SQL Server**. Incluye autenticación con sesión, dos perfiles, cartera persistente y decisiones auditadas.

## Probar las dos vistas

La aplicación integrada se abre en **http://127.0.0.1:8081**.

| Cuenta local | Perfil | Vista y permisos |
|---|---|---|
| `ejecutivo` | Ejecutivo comercial | Simulador, registro y seguimiento de sus propias solicitudes. |
| `aprobador` | Gestor de riesgos | Bandeja de la entidad, asignación de expedientes, aprobación y rechazo con fundamento. |

Las contraseñas son aleatorias y se guardan cifradas con DPAPI, fuera de Git. Para verlas en tu terminal privada:

```powershell
./scripts/Show-LocalAccess.ps1
```

Usa dos perfiles de navegador para mantener ambas sesiones abiertas a la vez, o **Cerrar sesión** para cambiar de cuenta. Los botones de perfil de la pantalla de acceso solo completan la matrícula; los permisos proceden del servidor.

## Arranque local

Requisitos: Java 17, Node/npm, dependencias Angular instaladas y SQL Server en el contenedor Podman `gastos_etl_mssql`, puerto 14330.

```powershell
cd frontend-angular
npm.cmd ci
cd ..
./scripts/Start-Local.ps1
```

El script aplica la migración V2 conservando los datos, inserta catálogos/clientes de demostración sin duplicarlos, compila Angular dentro del JAR, provisiona una cuenta SQL restringida y crea las dos cuentas locales. No restablece las claves de cuentas existentes. Java se inicia oculto; logs y PID quedan en `artifacts/private`.

Para detener solo la aplicación: `./scripts/Stop-Local.ps1`. SQL Server sigue activo.

Durante el desarrollo puedes usar `npm.cmd start -- --host 127.0.0.1` en `frontend-angular`; el frontend en el puerto 4200 reenvía `/api` al 8081.

## Flujo

1. Ejecutivo: elige un producto del catálogo, configura monto/inicial/plazo y selecciona clientes existentes. Los ingresos se obtienen del servidor.
2. Registra una o más garantías y guarda. El servidor genera el expediente y fija tasa, ingresos, fecha, estado y autor.
3. Gestor: consulta la bandeja, abre un expediente y pulsa **Iniciar evaluación** para asignárselo.
4. Revisa participantes, garantías, cuota, LTV, DSTI e historial. Aprueba o rechaza con fundamento obligatorio.
5. Ejecutivo: ve la decisión al actualizar o volver a abrir su cartera. Los datos permanecen después de recargar.

Para una prueba aprobable con los datos locales: vivienda S/ 150 000, inicial S/ 50 000, 20 años, producto tradicional, titular Juan Perez y garantía de S/ 150 000. Son datos y tasas ilustrativos.

## Validación

```powershell
$env:JAVA_HOME = 'C:\Program Files\Java\jdk-17'
./gradlew.bat test bootJar
cd frontend-angular
$env:CHROME_BIN = 'C:\Program Files\Google\Chrome\Application\chrome.exe'
npm.cmd test -- --watch=false --browsers=ChromeHeadless
cd ..
./scripts/Test-RoleBrowser.ps1
```

La prueba de navegador usa SQL Server real y dos sesiones independientes; crea dos solicitudes identificadas como **Prueba UI roles**, una aprobada y otra rechazada. No elimina registros. Evidencias en `artifacts/roles-review`.

Las pruebas Java incluyen login, CSRF, propiedad de cartera, permisos, validación, snapshots financieros, historial y asignación concurrente. Utilizan H2 para aislamiento; la revisión real complementa esa cobertura con SQL Server.

## Despliegue

`bootJar` incorpora la SPA y la API en el mismo origen. Activa el perfil `prod`, configura `DB_URL`, `DB_USERNAME`, `DB_PASSWORD`, sirve detrás de HTTPS y usa un certificado SQL válido. Aplica las migraciones con una cuenta administrativa separada; la aplicación no recibe permisos DDL ni DELETE.

Las dos cuentas automáticas existen únicamente bajo el perfil `local`. Para producción, provisiona empleados con hashes BCrypt y roles válidos. Las sesiones viven en la instancia Java; varias réplicas requieren afinidad o un almacén de sesiones compartido.

La política actual usa LTV máximo 90%, DSTI máximo 40% y plazo máximo 360 meses; no incluye seguros, comisiones, otras deudas, score externo ni tasas preferenciales. Requiere validación comercial antes de ofrecer crédito real.

Documentación: [arquitectura](docs/architecture.md), [API](docs/api.md), [base de datos](docs/database.md), [frontend](frontend-angular/README.md).
