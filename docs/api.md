# API actual de solicitudes V2

Contrato formal: [OpenAPI](openapi.yaml).

La API usa sesión de servidor, no Basic Auth. Todas las mutaciones requieren CSRF. Las respuestas nunca incluyen hashes de contraseña.

## Acceso

1. GET /api/csrf devuelve {token, headerName} y establece la sesión necesaria.
2. POST /api/login con Content-Type application/x-www-form-urlencoded, username/password y el header CSRF. Devuelve 200 o 401.
3. GET /api/session devuelve empleadoId, codigoMatricula, nombres, apellidos y rolPrincipal.
4. Después del login y antes de otra escritura, obtener un nuevo CSRF; el token anterior se invalida durante la autenticación.
5. POST /api/logout con CSRF devuelve 204 e invalida la sesión.

| Operación | Permiso |
|---|---|
| GET /api/productos | Ambos roles |
| GET /api/clientes?q=nombre-o-documento | Ejecutivo; máximo 20 coincidencias |
| POST /api/solicitudes/registrar | Ejecutivo |
| GET /api/solicitudes?page=0&size=12&estado=&q= | Ejecutivo: cartera propia; gestor: cartera de la entidad |
| GET /api/solicitudes/{id} | Ejecutivo propietario o gestor |
| POST /api/solicitudes/{id}/evaluar | Gestor |
| POST /api/solicitudes/{id}/aprobar | Gestor asignado |
| POST /api/solicitudes/{id}/rechazar | Gestor asignado |

La lista devuelve {content,totalElements,totalPages,number}. Size: 1–50. Estados: REGISTRADO, EN_EVALUACION, APROBADO, RECHAZADO. Búsqueda por expediente, nombre o documento, máximo 100 caracteres.

## Registro

```json
{
  "productoId": 1,
  "montoSolicitado": 100000,
  "plazoMeses": 240,
  "participantes": [{"clienteId": 1, "tipoParticipacion": "TITULAR"}],
  "inmuebles": [{
    "tipoInmueble": "DEPARTAMENTO",
    "direccion": "Dirección del inmueble",
    "partidaRegistral": "",
    "valorComercial": 150000,
    "valorTasacion": null
  }]
}
```

Devuelve 201. Los IDs son referencias a catálogos existentes. No se acepta tasa, empleado, estado, fecha, historial ni expediente suministrado por el cliente. Los ingresos y tasa se leen y conservan en servidor. Campos desconocidos son rechazados.

Participación: TITULAR o CODEUDOR, un titular obligatorio. Tipo de inmueble: DEPARTAMENTO, CASA o TERRENO. Importes positivos, máximo 10 enteros y 2 decimales; hasta cinco participantes/inmuebles.

## Transiciones

Evaluar: {"version":0}. Aprobar/rechazar: {"version":1,"comentario":"Fundamento de la decisión"}.

Version obligatoria. Comentario de 1–255 caracteres útiles. Se devuelve el detalle actualizado con nueva versión e historial. El servidor toma al empleado de la sesión, aunque se envíen parámetros de actor adicionales.

El detalle contiene cuotaMensual, ltv y dsti calculados en Java, además de participantes, garantías, ejecutivo, gestor e historial.

## Errores

400: formato o validación; 401: sesión inválida; 403: rol/CSRF/gestor ajeno; 404: no existe o no pertenece al ejecutivo; 409: versión, estado, límites de aprobación o integridad en conflicto; 503: recurso temporalmente no disponible.

No reintentar automáticamente un registro tras una respuesta incierta: consultar primero la cartera.
