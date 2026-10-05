USE Cotizador;
GO
IF NOT EXISTS (SELECT * FROM sys.tables WHERE name = 'Auditoria_Cotizaciones' AND schema_id = SCHEMA_ID('Cotizador'))
BEGIN
    CREATE TABLE Cotizador.Auditoria_Cotizaciones (
        id_auditoria INT IDENTITY(1,1) PRIMARY KEY,
        id_cotizacion INT NOT NULL FOREIGN KEY REFERENCES Cotizador.Cotizaciones(id_cotizacion),
        accion VARCHAR(100) NOT NULL,
        tea_anterior DECIMAL(5,2),
        tea_nueva DECIMAL(5,2),
        usuario_responsable VARCHAR(50),
        fecha_evento DATETIME NOT NULL
    );
END
GO
