USE Cotizador;
GO
SET XACT_ABORT ON;
BEGIN TRANSACTION;
IF SCHEMA_ID(N'Cotizador') IS NULL
    EXEC(N'CREATE SCHEMA Cotizador AUTHORIZATION dbo');

IF OBJECT_ID(N'Cotizador.Usuarios', N'U') IS NULL
CREATE TABLE Cotizador.Usuarios (
    id_usuario INT IDENTITY(1,1) PRIMARY KEY,
    username VARCHAR(50) NOT NULL UNIQUE,
    password_hash VARCHAR(255) NOT NULL,
    rol VARCHAR(20) NOT NULL
);

IF OBJECT_ID(N'Cotizador.Clientes', N'U') IS NULL
CREATE TABLE Cotizador.Clientes (
    id_cliente INT IDENTITY(1,1) PRIMARY KEY,
    dni VARCHAR(8) NOT NULL UNIQUE,
    nombres VARCHAR(100) NOT NULL,
    apellidos VARCHAR(100) NOT NULL,
    score_crediticio INT,
    ingresos_mensuales DECIMAL(18,2)
);

IF OBJECT_ID(N'Cotizador.Cotizaciones', N'U') IS NULL
CREATE TABLE Cotizador.Cotizaciones (
    id_cotizacion INT IDENTITY(1,1) PRIMARY KEY,
    id_cliente INT FOREIGN KEY REFERENCES Cotizador.Clientes(id_cliente),
    id_ejecutivo INT FOREIGN KEY REFERENCES Cotizador.Usuarios(id_usuario),
    valor_inmueble DECIMAL(18,2) NOT NULL,
    cuota_inicial DECIMAL(18,2) NOT NULL,
    monto_prestamo DECIMAL(18,2) NOT NULL,
    plazo_meses INT NOT NULL,
    ltv_porcentaje DECIMAL(5,2) NOT NULL,
    tea_calculada DECIMAL(5,2) NOT NULL,
    tea_preferencial_solicitada DECIMAL(5,2) NULL,
    cuota_mensual_estimada DECIMAL(18,2) NOT NULL,
    estado VARCHAR(20) DEFAULT 'BORRADOR',
    fecha_creacion DATETIME DEFAULT GETDATE()
);

IF NOT EXISTS (SELECT 1 FROM sys.indexes WHERE object_id = OBJECT_ID(N'Cotizador.Cotizaciones') AND name = N'IX_Cotizaciones_Estado_Fecha')
CREATE INDEX IX_Cotizaciones_Estado_Fecha ON Cotizador.Cotizaciones(estado, fecha_creacion);
IF NOT EXISTS (SELECT 1 FROM sys.indexes WHERE object_id = OBJECT_ID(N'Cotizador.Cotizaciones') AND name = N'IX_Cotizaciones_Cliente')
CREATE INDEX IX_Cotizaciones_Cliente ON Cotizador.Cotizaciones(id_cliente);
IF NOT EXISTS (SELECT 1 FROM sys.indexes WHERE object_id = OBJECT_ID(N'Cotizador.Cotizaciones') AND name = N'IX_Cotizaciones_Ejecutivo')
CREATE INDEX IX_Cotizaciones_Ejecutivo ON Cotizador.Cotizaciones(id_ejecutivo);
COMMIT;
GO
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
