# Umbral · Frontend Angular

Interfaz de originación y riesgos con componentes standalone, formularios reactivos y recursos locales.

## Acceso y perfiles

- **Ejecutivo comercial:** simulador, catálogos de productos/clientes y cartera propia. Puede registrar solicitudes, pero no tomar decisiones.
- **Gestor de riesgos:** bandeja de solicitudes de la entidad, detalle financiero, asignación, aprobación y rechazo. No dispone del simulador ni del catálogo de clientes.
- El rol procede de `GET /api/session`. Los guards y menús adaptan la interfaz; el servidor aplica los permisos de forma independiente.
- Login de servidor con cookie HttpOnly y CSRF. Las contraseñas no se almacenan en el navegador ni se conservan en el servicio Angular.
- Al recargar, se restaura la sesión y se consulta la cartera persistida. Al cerrar sesión se invalida también en el servidor.

Las cuentas locales son `ejecutivo` y `aprobador`. Obtén sus contraseñas con `scripts/Show-LocalAccess.ps1` desde la raíz del proyecto.

## Ejecución

Desde esta carpeta:

```powershell
npm.cmd ci
npm.cmd start -- --host 127.0.0.1
npm.cmd run build
npm.cmd test -- --watch=false --browsers=ChromeHeadless
```

Desarrollo: http://127.0.0.1:4200 con proxy `/api` hacia 8081. Aplicación integrada: http://127.0.0.1:8081. `gradlew bootJar` compila e incorpora el frontend en el JAR.

## Funciones

- Simulación orientativa francesa, resumen de cuota, composición del crédito, LTV y DSTI.
- Productos y clientes reales del catálogo. Hasta cinco participantes y cinco garantías.
- Registro con DTO explícito; el servidor decide autor, expediente, fecha, tasa, ingresos y estado.
- Búsqueda, filtros y paginación del servidor: 12 solicitudes por página. Los contadores indican el contenido de la página visible.
- Detalle con cálculos Java, datos evaluados e historial.
- Decisiones con versión optimista y fundamento obligatorio. Solo el gestor asignado puede decidir.
- Estados vacíos, errores, carga, navegación por teclado y diseño adaptable.

La vista previa JavaScript es orientativa. El detalle usa los cálculos BigDecimal del servidor. Las tasas e ingresos de una solicitud guardada permanecen fijos aunque cambien los catálogos.

## Pruebas

Desde la raíz:

- `python scripts/test_frontend_ui.py`: acceso público y adaptación a cinco tamaños, sin escrituras.
- `./scripts/Test-RoleBrowser.ps1`: flujo real de ejecutivo/gestor contra SQL Server; crea dos expedientes identificados como pruebas.
- Evidencias: `artifacts/roles-review`.

El despliegue de producción necesita HTTPS y configuración del perfil `prod`; consulta el README raíz. Los problemas anteriores de Basic Auth, empleados codificados, ausencia de consultas y mapeo de inmuebles fueron corregidos.
