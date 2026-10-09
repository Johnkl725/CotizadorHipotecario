# Arquitectura actual: Angular + solicitudes V2

## Capas

- Angular: login, guards por rol, simulador y bandeja de solicitudes. La SPA se empaqueta en el JAR y usa el mismo origen que la API.
- Adaptadores HTTP: DTOs validados, identidad desde Authentication y respuestas que excluyen hashes.
- Casos de uso: roles, propiedad de cartera, catálogo de productos/clientes, registro, consulta y decisiones.
- Dominio: entidades sin Spring/JPA, cálculo francés con BigDecimal, LTV/DSTI y máquina de estados.
- Adaptadores JPA: agregado de solicitud, referencias gestionadas, snapshots financieros y carga dentro de transacciones. Open-in-view desactivado.
- SQL Server: esquema Cotizador, FK calificadas, índices de cartera/estado y migración repetible.

## Autenticación y autorización

Spring Security valida BCrypt y crea una sesión de servidor. La SPA obtiene CSRF en /api/csrf antes de login y operaciones de escritura. La sesión rota al autenticar; logout invalida la sesión y elimina JSESSIONID. Cookie HttpOnly/SameSite; Secure en prod. Basic Auth está deshabilitado.

Los controladores reciben la matrícula autenticada. No reciben empleadoId/gestorId como autoridad. El caso de uso vuelve a validar el rol y la propiedad. Un ejecutivo solo lista/consulta sus expedientes. Los gestores consultan la cartera de la entidad; solo quien toma el expediente puede decidir.

Los clientes HTTP reciben DTOs públicos de empleado, nunca passwordHash. Los endpoints de Actuator, salvo salud, no son accesibles públicamente. No se habilitan credenciales CORS.

## Persistencia y concurrencia

Registro y decisiones son transaccionales. Al registrar se leen producto y clientes del catálogo; se conservan tasa_aplicada e ingreso_evaluado. Las fechas y número de expediente se generan en servidor.

En transiciones se modifica la entidad gestionada y se agregan entradas de historial. No se reemplazan ni borran los hijos persistidos. @Version incrementa la versión; el cliente debe enviar la versión leída. Tanto el control explícito como el UPDATE optimista evitan decisiones concurrentes. Un conflicto devuelve 409.

La paginación aplica al agregado raíz, sin fetch join de varias colecciones. Batch fetching de asociaciones está limitado a 50. La respuesta se construye dentro de la transacción.

Estados: REGISTRADO → EN_EVALUACION → APROBADO o RECHAZADO. No hay reapertura de decisiones finales.

## Cálculo y límites

Método francés, TEA convertida a tasa mensual con raíz duodécima, BigDecimal con precisión 34 y cuota a dos decimales. El LTV utiliza la suma del menor valor comercial/tasación; DSTI utiliza cuota e ingresos consolidados. Los límites se comparan sin redondear los ratios de presentación. No incluye otras deudas ni seguros.

Hasta cinco participantes, exactamente un titular y sin clientes repetidos; hasta cinco garantías. Moneda PEN y plazo 1–360. El registro acepta escenarios que requieren evaluación; la aprobación exige LTV ≤ 90% y DSTI ≤ 40%.

## Operación

El arranque local usa Podman, cuenta SQL restringida y claves aleatorias cifradas con DPAPI. La migración no elimina tablas. Los datos demostrativos son optativos y no sobrescriben datos existentes.

Producción requiere TLS, certificado SQL confiable, credenciales externas, aprovisionamiento de cuentas y validación comercial. El perfil prod activa cookies Secure. Las sesiones son locales a la instancia; no se ha configurado un almacén distribuido ni un despliegue público.

## Evidencia

RolesWorkflowIntegrationTest verifica login real, CSRF, permisos, aislamiento, snapshots, auditoría, validación y concurrencia en H2. Test-RoleBrowser.ps1 cubre registro, aprobación, rechazo, recarga, protección de rutas y logout con dos sesiones contra SQL Server. No equivale a un ensayo de capacidad o alta disponibilidad.
