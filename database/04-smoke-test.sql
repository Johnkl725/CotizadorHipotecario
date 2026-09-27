USE Cotizador;
GO
SET NOCOUNT ON;
SET XACT_ABORT ON;
IF (SELECT COUNT(*) FROM sys.tables WHERE schema_id = SCHEMA_ID(N'Cotizador') AND name IN ('Usuarios', 'Clientes', 'Cotizaciones')) <> 3
    THROW 51000, 'Faltan tablas del esquema Cotizador.', 1;
BEGIN TRY
    BEGIN TRANSACTION;
    DECLARE @username VARCHAR(50) = 'smoke-' + CONVERT(VARCHAR(36), NEWID());
    INSERT INTO Cotizador.Usuarios(username, password_hash, rol)
    VALUES (@username, 'SMOKE_TEST_NO_LOGIN', 'EJECUTIVO');
    DECLARE @ejecutivo INT = SCOPE_IDENTITY();
    DECLARE @dni VARCHAR(8);
    WHILE @dni IS NULL OR EXISTS (SELECT 1 FROM Cotizador.Clientes WHERE dni = @dni)
        SET @dni = LEFT(REPLACE(CONVERT(VARCHAR(36), NEWID()), '-', ''), 8);
    INSERT INTO Cotizador.Clientes(dni, nombres, apellidos, score_crediticio, ingresos_mensuales)
    VALUES (@dni, 'Prueba', 'Temporal', 800, 10000.00);
    DECLARE @cliente INT = SCOPE_IDENTITY();
    INSERT INTO Cotizador.Cotizaciones(id_cliente, id_ejecutivo, valor_inmueble, cuota_inicial,
        monto_prestamo, plazo_meses, ltv_porcentaje, tea_calculada, cuota_mensual_estimada,
        ingresos_mensuales, deudas_mensuales, score_crediticio, dsti_porcentaje)
    VALUES (@cliente, @ejecutivo, 300000.00, 60000.00, 240000.00, 240, 80.00, 0.00, 1000.00,
        10000.00, 500.00, 800, 15.00);
    DECLARE @cotizacion INT = SCOPE_IDENTITY();
    IF NOT EXISTS (SELECT 1 FROM Cotizador.Cotizaciones WHERE id_cotizacion = SCOPE_IDENTITY()
        AND estado = 'BORRADOR' AND fecha_creacion IS NOT NULL AND monto_prestamo = 240000.00
        AND version = 0 AND ingresos_mensuales = 10000.00 AND deudas_mensuales = 500.00
        AND score_crediticio = 800 AND dsti_porcentaje = 15.00)
        THROW 51001, 'Fallo de insercion o valores predeterminados.', 1;
    INSERT INTO Cotizador.Usuarios(username, password_hash, rol)
    VALUES ('approver-' + CONVERT(VARCHAR(36), NEWID()), 'SMOKE_TEST_NO_LOGIN', 'APROBADOR');
    DECLARE @aprobador INT = SCOPE_IDENTITY();
    UPDATE Cotizador.Cotizaciones SET version = version + 1, id_aprobador = @aprobador,
        comentario_decision = N'Prueba temporal de auditoria', fecha_decision = SYSUTCDATETIME()
    WHERE id_cotizacion = @cotizacion AND version = 0;
    IF @@ROWCOUNT <> 1 THROW 51002, 'No se actualizo la version inicial.', 1;
    UPDATE Cotizador.Cotizaciones SET version = version + 1
    WHERE id_cotizacion = @cotizacion AND version = 0;
    IF @@ROWCOUNT <> 0 THROW 51003, 'Se acepto una version obsoleta.', 1;
    IF NOT EXISTS (SELECT 1 FROM Cotizador.Cotizaciones WHERE id_cotizacion = @cotizacion
        AND version = 1 AND id_aprobador = @aprobador AND fecha_decision IS NOT NULL
        AND comentario_decision = N'Prueba temporal de auditoria')
        THROW 51004, 'Fallo en auditoria de decision.', 1;
    ROLLBACK;
    PRINT 'OK: tablas, relaciones e insercion; registros de prueba revertidos.';
END TRY
BEGIN CATCH
    IF @@TRANCOUNT > 0 ROLLBACK;
    THROW;
END CATCH;
GO
