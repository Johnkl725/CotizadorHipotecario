# Contratos REST — @ArchitectAgent / @BackendAgent

Contrato formal: [OpenAPI 3.0.3](openapi.yaml).

Base `/api`. Autenticación de sesión mediante formulario `POST /login`; CSRF también obligatorio en login/logout. Para JSON, obtener `/api/session` y enviar su `csrfToken` en el header indicado por `csrfHeader`. No se usa Basic Auth ni se desactiva CSRF. `/login` entrega el token del formulario en un input hidden.

| Método y ruta | Rol | Resultado |
|---|---|---|
| GET /session | Ambos | username, role, csrfToken, csrfHeader |
| GET /politica | Ambos | Umbrales y TEA base configurados |
| POST /simulaciones | EJECUTIVO | Resultado financiero sin persistir |
| POST /cotizaciones | EJECUTIVO | 201 y cotización guardada |
| GET /cotizaciones?page=0&size=12 | EJECUTIVO | Cartera propia paginada |
| POST /cotizaciones/{id}/solicitud | EJECUTIVO | BORRADOR → PENDIENTE_APROBACION |
| GET /aprobaciones?estado=PENDIENTE_APROBACION&page=0&size=12 | APROBADOR | Bandeja paginada; admite también APROBADA/RECHAZADA |
| POST /aprobaciones/{id}/decision | APROBADOR | Decisión final y cuota recalculada si aprueba |

Simulación:

```json
{"valorInmueble":"300000.00","cuotaInicial":"60000.00","plazoMeses":240,"ingresosMensuales":"12000.00","deudasMensuales":"500.00","scoreCrediticio":850}
```

Importes pueden enviarse como números JSON o strings decimales. Se recomiendan strings para evitar pérdida de precisión en clientes JavaScript. Score puede ser null; resto es obligatorio. Se aceptan hasta 9 dígitos enteros y 2 decimales, valores positivos para inmueble/ingresos, cero permitido para inicial/deudas. Plazo 1–360 sujeto al máximo configurado. Inicial debe cumplir política y ser menor al inmueble.

Respuesta: `montoPrestamo`, `ltvPorcentaje`, `tea`, `tem`, `cuotaMensual`, `dstiPorcentaje`, `totalIntereses`, `elegiblePreferencial`, `motivos`. Tasas y ratios porcentuales. Para el ejemplo con TEA 9%: cuota **2105.43**.

Crear cotización añade `dni` (8 dígitos), `nombres`, `apellidos` (máximo 100 caracteres). El DNI existente requiere el mismo nombre, sin distinguir mayúsculas. Cada cotización devuelve su `id` y `version`.

Solicitud de tasa:

```json
{"teaPreferencial":"8.00","version":0}
```

Decisión:

```json
{"aprobar":true,"comentario":"Perfil validado; capacidad de pago suficiente.","version":1}
```

Comentario obligatorio, máximo 500 caracteres. Siempre enviar la versión recién leída. Páginas: `{content,totalElements,totalPages,number}`, orden fecha/id descendente. Cotización incluye datos de cliente, financiero, estado, versión, ejecutivo y decisión; nunca hashes de contraseña.

Errores: `{timestamp,status,message,fieldErrors}` (los errores del filtro de seguridad omiten timestamp). 400 datos inválidos; 401 sin sesión; 403 rol/CSRF; 404 no existe o pertenece a otro ejecutivo; 409 estado/versión/identidad en conflicto; 422 perfil no elegible; 503 recurso temporalmente ocupado. No reintentar POST de creación automáticamente después de una respuesta incierta.
