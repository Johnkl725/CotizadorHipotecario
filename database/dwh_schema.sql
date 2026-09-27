-- HU 2: Estructura del Data Mart (Modelo Estrella) para el proceso PySpark/ELT
CREATE SCHEMA Estrella;
GO

-- 1. Dimensión Tiempo
CREATE TABLE Estrella.Dim_Tiempo (
    id_tiempo_dim INT PRIMARY KEY, -- Formato YYYYMMDD
    fecha DATE NOT NULL,
    anio INT NOT NULL,
    trimestre INT NOT NULL,
    mes INT NOT NULL,
    dia INT NOT NULL
);

-- 2. Dimensión Cliente
CREATE TABLE Estrella.Dim_Cliente (
    id_cliente_dim INT IDENTITY(1,1) PRIMARY KEY,
    id_cliente_origen INT NOT NULL, -- Surrogate key map
    rango_ingresos VARCHAR(50) NOT NULL, -- ej: '0 - 2000', '2001 - 5000'
    rango_score VARCHAR(50) NOT NULL, -- ej: '< 700 (Malo)', '700-750 (Bueno)'
    fecha_carga DATETIME DEFAULT GETDATE()
);

-- 3. Dimensión Ejecutivo
CREATE TABLE Estrella.Dim_Ejecutivo (
    id_ejecutivo_dim INT IDENTITY(1,1) PRIMARY KEY,
    id_usuario_origen INT NOT NULL,
    username VARCHAR(50) NOT NULL,
    rol VARCHAR(20) NOT NULL
);

-- 4. Dimensión Estado
CREATE TABLE Estrella.Dim_Estado (
    id_estado_dim INT IDENTITY(1,1) PRIMARY KEY,
    estado_cotizacion VARCHAR(50) NOT NULL
);

-- 5. Tabla de Hechos (Fact Table)
CREATE TABLE Estrella.Fact_Cotizaciones (
    id_fact INT IDENTITY(1,1) PRIMARY KEY,
    id_tiempo_dim INT FOREIGN KEY REFERENCES Estrella.Dim_Tiempo(id_tiempo_dim),
    id_cliente_dim INT FOREIGN KEY REFERENCES Estrella.Dim_Cliente(id_cliente_dim),
    id_ejecutivo_dim INT FOREIGN KEY REFERENCES Estrella.Dim_Ejecutivo(id_ejecutivo_dim),
    id_estado_dim INT FOREIGN KEY REFERENCES Estrella.Dim_Estado(id_estado_dim),
    
    -- Métricas / Medidas (Cantidades)
    monto_prestamo DECIMAL(18,2) NOT NULL,
    valor_inmueble DECIMAL(18,2) NOT NULL,
    cuota_inicial DECIMAL(18,2) NOT NULL,
    ltv_porcentaje DECIMAL(5,2) NOT NULL,
    dsti_porcentaje DECIMAL(9,2) NOT NULL,
    score_crediticio INT,
    tea_final DECIMAL(5,2) NOT NULL,
    cuota_mensual DECIMAL(18,2) NOT NULL
);
GO
