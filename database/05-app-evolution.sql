USE Cotizador;
GO
SET NOCOUNT ON;
SET XACT_ABORT ON;
BEGIN TRY
    BEGIN TRANSACTION;
    IF COL_LENGTH(N'Cotizador.Cotizaciones', N'version') IS NULL
        ALTER TABLE Cotizador.Cotizaciones ADD version BIGINT NOT NULL CONSTRAINT DF_Cotizaciones_Version DEFAULT (0) WITH VALUES;
    IF COL_LENGTH(N'Cotizador.Cotizaciones', N'ingresos_mensuales') IS NULL
    BEGIN
        ALTER TABLE Cotizador.Cotizaciones ADD ingresos_mensuales DECIMAL(18,2) NULL;
        EXEC(N'UPDATE q SET ingresos_mensuales = c.ingresos_mensuales FROM Cotizador.Cotizaciones q JOIN Cotizador.Clientes c ON c.id_cliente = q.id_cliente');
    END;
    IF COL_LENGTH(N'Cotizador.Cotizaciones', N'score_crediticio') IS NULL
    BEGIN
        ALTER TABLE Cotizador.Cotizaciones ADD score_crediticio INT NULL;
        EXEC(N'UPDATE q SET score_crediticio = c.score_crediticio FROM Cotizador.Cotizaciones q JOIN Cotizador.Clientes c ON c.id_cliente = q.id_cliente');
    END;
    IF COL_LENGTH(N'Cotizador.Cotizaciones', N'deudas_mensuales') IS NULL
    BEGIN
        ALTER TABLE Cotizador.Cotizaciones ADD deudas_mensuales DECIMAL(18,2) NULL;
        -- Historical debts were not recorded. Zero is a migration assumption, not verified debt.
        -- No default: new records must supply the actual declared debt explicitly.
        EXEC(N'UPDATE Cotizador.Cotizaciones SET deudas_mensuales = 0');
    END;
    IF COL_LENGTH(N'Cotizador.Cotizaciones', N'dsti_porcentaje') IS NULL
    BEGIN
        ALTER TABLE Cotizador.Cotizaciones ADD dsti_porcentaje DECIMAL(9,2) NULL;
        EXEC(N'UPDATE Cotizador.Cotizaciones SET dsti_porcentaje = TRY_CONVERT(DECIMAL(9,2), (cuota_mensual_estimada + deudas_mensuales) * 100.0 / NULLIF(ingresos_mensuales, 0)) WHERE ingresos_mensuales > 0');
    END;
    IF COL_LENGTH(N'Cotizador.Cotizaciones', N'id_aprobador') IS NULL
        ALTER TABLE Cotizador.Cotizaciones ADD id_aprobador INT NULL;
    IF COL_LENGTH(N'Cotizador.Cotizaciones', N'comentario_decision') IS NULL
        ALTER TABLE Cotizador.Cotizaciones ADD comentario_decision NVARCHAR(500) NULL;
    IF COL_LENGTH(N'Cotizador.Cotizaciones', N'fecha_decision') IS NULL
        ALTER TABLE Cotizador.Cotizaciones ADD fecha_decision DATETIME2 NULL;
    IF NOT EXISTS (SELECT 1 FROM sys.foreign_keys WHERE parent_object_id = OBJECT_ID(N'Cotizador.Cotizaciones') AND name = N'FK_Cotizaciones_Aprobador')
        EXEC(N'ALTER TABLE Cotizador.Cotizaciones WITH CHECK ADD CONSTRAINT FK_Cotizaciones_Aprobador FOREIGN KEY (id_aprobador) REFERENCES Cotizador.Usuarios(id_usuario)');
    IF NOT EXISTS (SELECT 1 FROM sys.indexes WHERE object_id = OBJECT_ID(N'Cotizador.Cotizaciones') AND name = N'IX_Cotizaciones_Ejecutivo_Pagina')
        CREATE INDEX IX_Cotizaciones_Ejecutivo_Pagina ON Cotizador.Cotizaciones(id_ejecutivo, fecha_creacion DESC, id_cotizacion DESC);
    IF NOT EXISTS (SELECT 1 FROM sys.indexes WHERE object_id = OBJECT_ID(N'Cotizador.Cotizaciones') AND name = N'IX_Cotizaciones_Estado_Pagina')
        CREATE INDEX IX_Cotizaciones_Estado_Pagina ON Cotizador.Cotizaciones(estado, fecha_creacion DESC, id_cotizacion DESC);
    COMMIT;
    PRINT 'OK: migracion de snapshots, auditoria, version e indices.';
END TRY
BEGIN CATCH
    IF @@TRANCOUNT > 0 ROLLBACK;
    THROW;
END CATCH;
GO
