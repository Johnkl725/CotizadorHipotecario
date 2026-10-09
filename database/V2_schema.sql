USE Cotizador;
GO
SET XACT_ABORT ON;
SET NOCOUNT ON;
IF SCHEMA_ID(N'Cotizador') IS NULL EXEC(N'CREATE SCHEMA Cotizador');
GO
-- Additive, repeatable V2 migration. Existing tables and data are retained.
BEGIN TRANSACTION;
GO

IF OBJECT_ID(N'Cotizador.producto_hipotecario', N'U') IS NULL
BEGIN
CREATE TABLE Cotizador.producto_hipotecario (
    producto_id SMALLINT IDENTITY(1,1) NOT NULL,
    nombre_producto VARCHAR(50) NOT NULL,
    tasa_interes_referencial DECIMAL(5,2) NOT NULL,
    CONSTRAINT pk_producto PRIMARY KEY (producto_id)
);
END;
GO

IF OBJECT_ID(N'Cotizador.cliente', N'U') IS NULL
BEGIN
CREATE TABLE Cotizador.cliente (
    cliente_id INT IDENTITY(1,1) NOT NULL,
    tipo_documento CHAR(3) NOT NULL,
    numero_documento VARCHAR(15) NOT NULL,
    nombres VARCHAR(50) NOT NULL,
    apellidos VARCHAR(50) NOT NULL,
    email VARCHAR(80) NULL,
    ingreso_mensual_neto DECIMAL(12,2) NOT NULL,
    CONSTRAINT pk_cliente PRIMARY KEY (cliente_id),
    CONSTRAINT uk_cliente_doc UNIQUE (tipo_documento, numero_documento)
);
END;
GO

IF OBJECT_ID(N'Cotizador.empleado', N'U') IS NULL
BEGIN
CREATE TABLE Cotizador.empleado (
    empleado_id INT IDENTITY(1,1) NOT NULL,
    codigo_matricula VARCHAR(10) NOT NULL,
    nombres VARCHAR(50) NOT NULL,
    apellidos VARCHAR(50) NOT NULL,
    rol_principal VARCHAR(30) NOT NULL,
    password_hash VARCHAR(255) NOT NULL,
    CONSTRAINT pk_empleado PRIMARY KEY (empleado_id),
    CONSTRAINT uk_empleado_matricula UNIQUE (codigo_matricula)
);
END;
GO

IF OBJECT_ID(N'Cotizador.solicitud_credito', N'U') IS NULL
BEGIN
CREATE TABLE Cotizador.solicitud_credito (
    solicitud_id INT IDENTITY(1,1) NOT NULL,
    numero_expediente VARCHAR(20) NOT NULL,
    producto_id SMALLINT NOT NULL,
    ejecutivo_id INT NOT NULL,
    gestor_riesgo_id INT NULL,
    monto_solicitado DECIMAL(12,2) NOT NULL,
    tasa_aplicada DECIMAL(5,2) NOT NULL,
    moneda CHAR(3) NOT NULL DEFAULT 'PEN',
    plazo_meses SMALLINT NOT NULL,
    estado_actual VARCHAR(30) NOT NULL DEFAULT 'REGISTRADO',
    fecha_creacion DATETIME2 NOT NULL DEFAULT CURRENT_TIMESTAMP,
    version BIGINT NOT NULL DEFAULT 0, -- Para Concurrencia Optimista
    CONSTRAINT pk_solicitud PRIMARY KEY (solicitud_id),
    CONSTRAINT uk_solicitud_expediente UNIQUE (numero_expediente),
    CONSTRAINT fk_solicitud_producto FOREIGN KEY (producto_id) REFERENCES Cotizador.producto_hipotecario (producto_id),
    CONSTRAINT fk_solicitud_ejecutivo FOREIGN KEY (ejecutivo_id) REFERENCES Cotizador.empleado (empleado_id),
    CONSTRAINT fk_solicitud_gestor FOREIGN KEY (gestor_riesgo_id) REFERENCES Cotizador.empleado (empleado_id)
);
END;
GO

IF OBJECT_ID(N'Cotizador.solicitud_cliente', N'U') IS NULL
BEGIN
CREATE TABLE Cotizador.solicitud_cliente (
    solicitud_id INT NOT NULL,
    cliente_id INT NOT NULL,
    ingreso_evaluado DECIMAL(12,2) NOT NULL,
    tipo_participacion VARCHAR(20) NOT NULL,
    CONSTRAINT pk_solicitud_cliente PRIMARY KEY (solicitud_id, cliente_id),
    CONSTRAINT fk_sol_cli_solicitud FOREIGN KEY (solicitud_id) REFERENCES Cotizador.solicitud_credito (solicitud_id),
    CONSTRAINT fk_sol_cli_cliente FOREIGN KEY (cliente_id) REFERENCES Cotizador.cliente (cliente_id)
);
END;
GO

IF OBJECT_ID(N'Cotizador.inmueble_garantia', N'U') IS NULL
BEGIN
CREATE TABLE Cotizador.inmueble_garantia (
    inmueble_id INT IDENTITY(1,1) NOT NULL,
    solicitud_id INT NOT NULL,
    tipo_inmueble VARCHAR(30) NOT NULL,
    direccion VARCHAR(150) NOT NULL,
    partida_registral VARCHAR(30) NULL,
    valor_comercial DECIMAL(12,2) NOT NULL,
    valor_tasacion DECIMAL(12,2) NULL,
    CONSTRAINT pk_inmueble PRIMARY KEY (inmueble_id),
    CONSTRAINT fk_inmueble_solicitud FOREIGN KEY (solicitud_id) REFERENCES Cotizador.solicitud_credito (solicitud_id)
);
END;
GO

IF OBJECT_ID(N'Cotizador.solicitud_historial_estado', N'U') IS NULL
BEGIN
CREATE TABLE Cotizador.solicitud_historial_estado (
    historial_id INT IDENTITY(1,1) NOT NULL,
    solicitud_id INT NOT NULL,
    empleado_id INT NOT NULL,
    estado_anterior VARCHAR(30) NULL,
    estado_nuevo VARCHAR(30) NOT NULL,
    fecha_cambio DATETIME2 NOT NULL DEFAULT CURRENT_TIMESTAMP,
    comentario VARCHAR(255) NULL,
    CONSTRAINT pk_historial PRIMARY KEY (historial_id),
    CONSTRAINT fk_hist_solicitud FOREIGN KEY (solicitud_id) REFERENCES Cotizador.solicitud_credito (solicitud_id),
    CONSTRAINT fk_hist_empleado FOREIGN KEY (empleado_id) REFERENCES Cotizador.empleado (empleado_id)
);
END;
GO

IF COL_LENGTH('Cotizador.solicitud_credito','tasa_aplicada') IS NULL
    ALTER TABLE Cotizador.solicitud_credito ADD tasa_aplicada DECIMAL(5,2) NULL;
IF COL_LENGTH('Cotizador.solicitud_cliente','ingreso_evaluado') IS NULL
    ALTER TABLE Cotizador.solicitud_cliente ADD ingreso_evaluado DECIMAL(12,2) NULL;
GO
UPDATE s SET tasa_aplicada = p.tasa_interes_referencial
FROM Cotizador.solicitud_credito s JOIN Cotizador.producto_hipotecario p ON p.producto_id=s.producto_id
WHERE s.tasa_aplicada IS NULL;
UPDATE p SET ingreso_evaluado = c.ingreso_mensual_neto
FROM Cotizador.solicitud_cliente p JOIN Cotizador.cliente c ON c.cliente_id=p.cliente_id
WHERE p.ingreso_evaluado IS NULL;
ALTER TABLE Cotizador.solicitud_credito ALTER COLUMN tasa_aplicada DECIMAL(5,2) NOT NULL;
ALTER TABLE Cotizador.solicitud_cliente ALTER COLUMN ingreso_evaluado DECIMAL(12,2) NOT NULL;
IF NOT EXISTS (SELECT 1 FROM sys.indexes WHERE name='ix_solicitud_ejecutivo_fecha' AND object_id=OBJECT_ID('Cotizador.solicitud_credito'))
    CREATE INDEX ix_solicitud_ejecutivo_fecha ON Cotizador.solicitud_credito(ejecutivo_id, fecha_creacion DESC, solicitud_id DESC);
IF NOT EXISTS (SELECT 1 FROM sys.indexes WHERE name='ix_solicitud_estado_fecha' AND object_id=OBJECT_ID('Cotizador.solicitud_credito'))
    CREATE INDEX ix_solicitud_estado_fecha ON Cotizador.solicitud_credito(estado_actual, fecha_creacion DESC, solicitud_id DESC);

-- Repair legacy V2 foreign keys that accidentally targeted dbo.
IF EXISTS (SELECT 1 FROM sys.foreign_keys WHERE name='fk_solicitud_producto' AND parent_object_id=OBJECT_ID('Cotizador.solicitud_credito')
    AND referenced_object_id<>OBJECT_ID('Cotizador.producto_hipotecario'))
BEGIN
    IF EXISTS (SELECT 1 FROM Cotizador.solicitud_credito s LEFT JOIN Cotizador.producto_hipotecario t ON t.producto_id=s.producto_id
        WHERE s.producto_id IS NOT NULL AND t.producto_id IS NULL)
        THROW 51120, 'Reconcile legacy references before migration: fk_solicitud_producto. No rows were deleted.', 1;
    ALTER TABLE Cotizador.solicitud_credito DROP CONSTRAINT fk_solicitud_producto;
END;
IF NOT EXISTS (SELECT 1 FROM sys.foreign_keys WHERE name='fk_solicitud_producto' AND parent_object_id=OBJECT_ID('Cotizador.solicitud_credito'))
    ALTER TABLE Cotizador.solicitud_credito WITH CHECK ADD CONSTRAINT fk_solicitud_producto
        FOREIGN KEY (producto_id) REFERENCES Cotizador.producto_hipotecario(producto_id);


-- Repair legacy V2 foreign keys that accidentally targeted dbo.
IF EXISTS (SELECT 1 FROM sys.foreign_keys WHERE name='fk_solicitud_ejecutivo' AND parent_object_id=OBJECT_ID('Cotizador.solicitud_credito')
    AND referenced_object_id<>OBJECT_ID('Cotizador.empleado'))
BEGIN
    IF EXISTS (SELECT 1 FROM Cotizador.solicitud_credito s LEFT JOIN Cotizador.empleado t ON t.empleado_id=s.ejecutivo_id
        WHERE s.ejecutivo_id IS NOT NULL AND t.empleado_id IS NULL)
        THROW 51120, 'Reconcile legacy references before migration: fk_solicitud_ejecutivo. No rows were deleted.', 1;
    ALTER TABLE Cotizador.solicitud_credito DROP CONSTRAINT fk_solicitud_ejecutivo;
END;
IF NOT EXISTS (SELECT 1 FROM sys.foreign_keys WHERE name='fk_solicitud_ejecutivo' AND parent_object_id=OBJECT_ID('Cotizador.solicitud_credito'))
    ALTER TABLE Cotizador.solicitud_credito WITH CHECK ADD CONSTRAINT fk_solicitud_ejecutivo
        FOREIGN KEY (ejecutivo_id) REFERENCES Cotizador.empleado(empleado_id);


-- Repair legacy V2 foreign keys that accidentally targeted dbo.
IF EXISTS (SELECT 1 FROM sys.foreign_keys WHERE name='fk_solicitud_gestor' AND parent_object_id=OBJECT_ID('Cotizador.solicitud_credito')
    AND referenced_object_id<>OBJECT_ID('Cotizador.empleado'))
BEGIN
    IF EXISTS (SELECT 1 FROM Cotizador.solicitud_credito s LEFT JOIN Cotizador.empleado t ON t.empleado_id=s.gestor_riesgo_id
        WHERE s.gestor_riesgo_id IS NOT NULL AND t.empleado_id IS NULL)
        THROW 51120, 'Reconcile legacy references before migration: fk_solicitud_gestor. No rows were deleted.', 1;
    ALTER TABLE Cotizador.solicitud_credito DROP CONSTRAINT fk_solicitud_gestor;
END;
IF NOT EXISTS (SELECT 1 FROM sys.foreign_keys WHERE name='fk_solicitud_gestor' AND parent_object_id=OBJECT_ID('Cotizador.solicitud_credito'))
    ALTER TABLE Cotizador.solicitud_credito WITH CHECK ADD CONSTRAINT fk_solicitud_gestor
        FOREIGN KEY (gestor_riesgo_id) REFERENCES Cotizador.empleado(empleado_id);


-- Repair legacy V2 foreign keys that accidentally targeted dbo.
IF EXISTS (SELECT 1 FROM sys.foreign_keys WHERE name='fk_sol_cli_solicitud' AND parent_object_id=OBJECT_ID('Cotizador.solicitud_cliente')
    AND referenced_object_id<>OBJECT_ID('Cotizador.solicitud_credito'))
BEGIN
    IF EXISTS (SELECT 1 FROM Cotizador.solicitud_cliente s LEFT JOIN Cotizador.solicitud_credito t ON t.solicitud_id=s.solicitud_id
        WHERE s.solicitud_id IS NOT NULL AND t.solicitud_id IS NULL)
        THROW 51120, 'Reconcile legacy references before migration: fk_sol_cli_solicitud. No rows were deleted.', 1;
    ALTER TABLE Cotizador.solicitud_cliente DROP CONSTRAINT fk_sol_cli_solicitud;
END;
IF NOT EXISTS (SELECT 1 FROM sys.foreign_keys WHERE name='fk_sol_cli_solicitud' AND parent_object_id=OBJECT_ID('Cotizador.solicitud_cliente'))
    ALTER TABLE Cotizador.solicitud_cliente WITH CHECK ADD CONSTRAINT fk_sol_cli_solicitud
        FOREIGN KEY (solicitud_id) REFERENCES Cotizador.solicitud_credito(solicitud_id);


-- Repair legacy V2 foreign keys that accidentally targeted dbo.
IF EXISTS (SELECT 1 FROM sys.foreign_keys WHERE name='fk_sol_cli_cliente' AND parent_object_id=OBJECT_ID('Cotizador.solicitud_cliente')
    AND referenced_object_id<>OBJECT_ID('Cotizador.cliente'))
BEGIN
    IF EXISTS (SELECT 1 FROM Cotizador.solicitud_cliente s LEFT JOIN Cotizador.cliente t ON t.cliente_id=s.cliente_id
        WHERE s.cliente_id IS NOT NULL AND t.cliente_id IS NULL)
        THROW 51120, 'Reconcile legacy references before migration: fk_sol_cli_cliente. No rows were deleted.', 1;
    ALTER TABLE Cotizador.solicitud_cliente DROP CONSTRAINT fk_sol_cli_cliente;
END;
IF NOT EXISTS (SELECT 1 FROM sys.foreign_keys WHERE name='fk_sol_cli_cliente' AND parent_object_id=OBJECT_ID('Cotizador.solicitud_cliente'))
    ALTER TABLE Cotizador.solicitud_cliente WITH CHECK ADD CONSTRAINT fk_sol_cli_cliente
        FOREIGN KEY (cliente_id) REFERENCES Cotizador.cliente(cliente_id);


-- Repair legacy V2 foreign keys that accidentally targeted dbo.
IF EXISTS (SELECT 1 FROM sys.foreign_keys WHERE name='fk_inmueble_solicitud' AND parent_object_id=OBJECT_ID('Cotizador.inmueble_garantia')
    AND referenced_object_id<>OBJECT_ID('Cotizador.solicitud_credito'))
BEGIN
    IF EXISTS (SELECT 1 FROM Cotizador.inmueble_garantia s LEFT JOIN Cotizador.solicitud_credito t ON t.solicitud_id=s.solicitud_id
        WHERE s.solicitud_id IS NOT NULL AND t.solicitud_id IS NULL)
        THROW 51120, 'Reconcile legacy references before migration: fk_inmueble_solicitud. No rows were deleted.', 1;
    ALTER TABLE Cotizador.inmueble_garantia DROP CONSTRAINT fk_inmueble_solicitud;
END;
IF NOT EXISTS (SELECT 1 FROM sys.foreign_keys WHERE name='fk_inmueble_solicitud' AND parent_object_id=OBJECT_ID('Cotizador.inmueble_garantia'))
    ALTER TABLE Cotizador.inmueble_garantia WITH CHECK ADD CONSTRAINT fk_inmueble_solicitud
        FOREIGN KEY (solicitud_id) REFERENCES Cotizador.solicitud_credito(solicitud_id);


-- Repair legacy V2 foreign keys that accidentally targeted dbo.
IF EXISTS (SELECT 1 FROM sys.foreign_keys WHERE name='fk_hist_solicitud' AND parent_object_id=OBJECT_ID('Cotizador.solicitud_historial_estado')
    AND referenced_object_id<>OBJECT_ID('Cotizador.solicitud_credito'))
BEGIN
    IF EXISTS (SELECT 1 FROM Cotizador.solicitud_historial_estado s LEFT JOIN Cotizador.solicitud_credito t ON t.solicitud_id=s.solicitud_id
        WHERE s.solicitud_id IS NOT NULL AND t.solicitud_id IS NULL)
        THROW 51120, 'Reconcile legacy references before migration: fk_hist_solicitud. No rows were deleted.', 1;
    ALTER TABLE Cotizador.solicitud_historial_estado DROP CONSTRAINT fk_hist_solicitud;
END;
IF NOT EXISTS (SELECT 1 FROM sys.foreign_keys WHERE name='fk_hist_solicitud' AND parent_object_id=OBJECT_ID('Cotizador.solicitud_historial_estado'))
    ALTER TABLE Cotizador.solicitud_historial_estado WITH CHECK ADD CONSTRAINT fk_hist_solicitud
        FOREIGN KEY (solicitud_id) REFERENCES Cotizador.solicitud_credito(solicitud_id);


-- Repair legacy V2 foreign keys that accidentally targeted dbo.
IF EXISTS (SELECT 1 FROM sys.foreign_keys WHERE name='fk_hist_empleado' AND parent_object_id=OBJECT_ID('Cotizador.solicitud_historial_estado')
    AND referenced_object_id<>OBJECT_ID('Cotizador.empleado'))
BEGIN
    IF EXISTS (SELECT 1 FROM Cotizador.solicitud_historial_estado s LEFT JOIN Cotizador.empleado t ON t.empleado_id=s.empleado_id
        WHERE s.empleado_id IS NOT NULL AND t.empleado_id IS NULL)
        THROW 51120, 'Reconcile legacy references before migration: fk_hist_empleado. No rows were deleted.', 1;
    ALTER TABLE Cotizador.solicitud_historial_estado DROP CONSTRAINT fk_hist_empleado;
END;
IF NOT EXISTS (SELECT 1 FROM sys.foreign_keys WHERE name='fk_hist_empleado' AND parent_object_id=OBJECT_ID('Cotizador.solicitud_historial_estado'))
    ALTER TABLE Cotizador.solicitud_historial_estado WITH CHECK ADD CONSTRAINT fk_hist_empleado
        FOREIGN KEY (empleado_id) REFERENCES Cotizador.empleado(empleado_id);
COMMIT TRANSACTION;
GO
