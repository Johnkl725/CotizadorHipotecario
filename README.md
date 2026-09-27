# UMBRAL · New Cotizador

Aplicación hipotecaria en soles: simulación francesa con tasa fija, cartera por ejecutivo y aprobación de tasas preferenciales. Spring Boot 4.1.1, Java 17, SQL Server 2022 y Thymeleaf. Diseño propio en bosque, marfil y cobre, sin servicios de fuentes ni JavaScript externos.

## Arranque local (Windows / Podman)

Requisitos: Java 17 en PATH y el contenedor SQL Server `gastos_etl_mssql` activo, publicado en `localhost:14330`. No ocupa el puerto 8080 que utiliza Airflow.

```powershell
./scripts/Start-Local.ps1
./scripts/Show-LocalAccess.ps1
```

Abre **http://localhost:8081**. El primer comando aplica las migraciones, compila, provisiona una cuenta SQL restringida, genera claves aleatorias para `ejecutivo` y `aprobador`, e inicia Java oculto. El segundo muestra tus claves locales. Usa dos perfiles de navegador para trabajar simultáneamente con ambos roles.

Las credenciales se guardan cifradas con DPAPI bajo `artifacts/private`, excluido de Git. Solo el mismo usuario Windows puede descifrarlas. No hay contraseñas predeterminadas. Los usuarios existentes no se sobrescriben. Logs y PID están en ese directorio. No borres las credenciales mientras conserves las cuentas SQL/aplicación.

Para usar otra instancia, configura `DB_URL` y proporciona sus credenciales como variables de entorno; inicia el JAR directamente. `Start-Local.ps1` está destinado al entorno Podman descrito.

```powershell
$env:JAVA_HOME = 'C:\Program Files\Java\jdk-17'
./gradlew.bat test bootJar
# DB_URL, DB_USERNAME, DB_PASSWORD deben existir en el entorno de este proceso.
java -Xms128m -Xmx512m -jar build/libs/NewCotizador-0.0.1-SNAPSHOT.jar
```

## Flujo

1. Ejecutivo: introduce valor, inicial, plazo, ingresos, deudas existentes y score opcional. Calcula sin escribir en SQL.
2. Guarda la cotización con DNI y nombre. Se conservan sus variables financieras; nuevos ingresos no alteran cotizaciones anteriores.
3. Si el perfil cumple la política, solicita una TEA menor a la original.
4. Aprobador: revisa perfil, LTV, DSTI y score; aprueba o rechaza con fundamento obligatorio.
5. Una aprobación recalcula la cuota con la tasa concedida. Se conserva la TEA original y se registra quién decidió y cuándo.

Cambios simultáneos devuelven HTTP 409. Otro ejecutivo no puede consultar ni enviar solicitudes sobre cotizaciones ajenas. Rechazar conserva la cuota original. Esta versión admite una decisión final por solicitud; para otra evaluación se crea otra cotización.

## Política configurable de demostración

El diseño original no establece una política comercial. Se usa **TEA 9%, inicial mínima 10%, score mínimo 700, DSTI máximo 40%, plazo máximo 360 meses**. Son valores ilustrativos, no tasas de mercado ni una aprobación crediticia. Para preferencial se exige score **y** DSTI dentro de límites. Score desconocido permite simular y guardar, pero no solicitar preferencial.

Configura `TEA_BASE`, `INICIAL_MINIMA`, `SCORE_MINIMO`, `DSTI_MAXIMO`, `PLAZO_MAXIMO`. TEA/TEM/LTV/DSTI de la API son porcentajes, no fracciones. Los límites se comparan sin redondear el DSTI. Cuotas a dos decimales, HALF_UP; cálculo interno a 34 dígitos. Incluye deuda mensual existente. No incluye seguros, comisiones, periodos de gracia ni tasas variables/mixtas. Intereses totales son una estimación teórica antes del ajuste de la última cuota.

## Verificación

```powershell
./gradlew.bat test
./scripts/Test-LocalLoad.ps1 -Users 60 -Iterations 20
# Incluye escrituras y decisiones; deja 60 cotizaciones sintéticas identificadas como Carga.
./scripts/Test-LocalLoad.ps1 -Users 60 -Iterations 20 -Persist
# Opcional: Python con playwright y Chromium; crea una cotización sintética.
./scripts/Test-LocalBrowser.ps1
```

Las pruebas Java usan H2 en modo SQL Server para aislamiento y rapidez. Las pruebas HTTP y de navegador usan SQL Server real. Los reportes de carga se guardan en `artifacts/load-*.json` y las capturas en `artifacts/*.png`. No confundir concurrencia del cálculo puro con capacidad extremo a extremo: consulta los resultados y límites en [arquitectura](docs/architecture.md).

Documentación: [base de datos](docs/database.md), [API](docs/api.md), [arquitectura y operación](docs/architecture.md).
