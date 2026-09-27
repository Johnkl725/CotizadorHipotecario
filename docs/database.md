# @DatabaseAgent: migracion y operacion

Ejecutar `./database/Initialize-Cotizador.ps1` antes de iniciar la aplicacion. El orden es `01`, `02`, `05`, datos opcionales `03`, y smoke `04`. Se conserva el numero original de la prueba para compatibilidad. Ejecutar una sola migracion a la vez, durante una ventana de despliegue: ALTER TABLE requiere bloqueos de esquema. Hibernate debe usar `ddl-auto=validate`.

`05-app-evolution.sql` agrega a `Cotizador.Cotizaciones`:

| Columna | Tipo | Uso |
| --- | --- | --- |
| version | BIGINT NOT NULL DEFAULT 0 | Version de bloqueo optimista; JPA incrementa al actualizar |
| ingresos_mensuales | DECIMAL(18,2) NULL | Ingreso declarado al cotizar |
| deudas_mensuales | DECIMAL(18,2) NULL | Cuotas mensuales existentes |
| score_crediticio | INT NULL | Score declarado al cotizar |
| dsti_porcentaje | DECIMAL(9,2) NULL | (Cuota + deudas) / ingreso * 100 |
| id_aprobador | INT NULL | FK a Usuarios |
| comentario_decision | NVARCHAR(500) NULL | Motivo de decision |
| fecha_decision | DATETIME2 NULL | Instante de decision, enviado en UTC por la app |

El backfill ocurre solamente al crear cada columna: ingreso y score se copian del cliente actual, porque no existe historia anterior; deudas se inicializa en cero exclusivamente para registros antiguos. **El cero historico representa informacion desconocida, no ausencia de deuda verificada.** Su DSTI es solo una estimacion con ese supuesto; revisar los datos antes de decidir sobre solicitudes antiguas. Ingresos no positivos producen DSTI nulo. No se inventan aprobadores, comentarios ni fechas para decisiones historicas. Repetir el script no altera snapshots ya existentes. Las cotizaciones nuevas deben enviar sus propios datos de riesgo; no existen defaults que oculten datos faltantes.

Los indices `(id_ejecutivo, fecha_creacion DESC, id_cotizacion DESC)` y `(estado, fecha_creacion DESC, id_cotizacion DESC)` permiten paginar con orden estable. Se conservan los indices originales para no eliminar dependencias existentes. Revisar planes y uso antes de retirar indices redundantes en produccion.

El smoke crea datos dentro de una transaccion y los revierte. Verifica snapshots, default de version, FK de aprobador, auditoria y que una actualizacion con version obsoleta afecte cero filas. No reemplaza la prueba de concurrencia HTTP. Los contadores IDENTITY pueden avanzar aunque se revierta la transaccion.

## Cuenta SQL de ejecucion

La cuenta de migraciones necesita DDL; debe ser distinta de la cuenta de la app. El inicializador local reutiliza SA dentro del contenedor sin imprimir su clave. No usar SA para servir peticiones web. Un administrador debe provisionar un login y usuario propios y otorgar solamente:

```sql
USE Cotizador;
-- Sustituir CotizadorApp por el usuario previamente provisionado.
GRANT SELECT ON OBJECT::Cotizador.Usuarios TO CotizadorApp;
GRANT SELECT, INSERT, UPDATE ON OBJECT::Cotizador.Clientes TO CotizadorApp;
GRANT SELECT, INSERT, UPDATE ON OBJECT::Cotizador.Cotizaciones TO CotizadorApp;
```

La provision de cuentas web debe ejecutarse separadamente con permisos de escritura en Usuarios. No otorgar `db_owner`, `ALTER`, `CONTROL` ni DELETE a la cuenta de ejecucion. Mantener credenciales en variables de entorno o gestor de secretos. En produccion usar certificado SQL valido con `encrypt=true;trustServerCertificate=false`, backups y recursos medidos. El numero de usuarios concurrentes por si solo no demuestra capacidad: medir latencia, saturacion del pool, CPU y bloqueos con la carga real.
