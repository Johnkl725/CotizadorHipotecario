import os
from pyspark.sql import SparkSession
from pyspark.sql.functions import col, current_date, year, month, quarter, dayofmonth

# Inicializar Spark con soporte para Delta Lake y SQL Server (JDBC)
# Nota: Esta configuración asume que el .bat ya inició el contenedor con los JARs necesarios
spark = SparkSession.builder \
    .appName("ETL_Cotizador_Medallion") \
    .getOrCreate()

# Configuraciones de conexión a DB Transaccional (Origen) y DWH (Destino)
JDBC_URL_ORIGEN = "jdbc:sqlserver://host.containers.internal:14330;databaseName=Cotizador;trustServerCertificate=true;"
JDBC_URL_DESTINO = "jdbc:sqlserver://host.containers.internal:14330;databaseName=Cotizador;trustServerCertificate=true;"
DB_USER = os.getenv("DB_USERNAME", "cotizador_app")
DB_PASS = os.getenv("DB_PASSWORD", "tu_password")

# ==========================================
# CAPA BRONZE (Staging - Datos Crudos)
# ==========================================
print("Extrayendo datos a Capa Bronze...")
# 1. Leer de SQL Server
df_cotizaciones_raw = spark.read.format("jdbc") \
    .option("url", JDBC_URL_ORIGEN) \
    .option("dbtable", "Cotizador.Cotizaciones") \
    .option("user", DB_USER).option("password", DB_PASS).load()

df_clientes_raw = spark.read.format("jdbc") \
    .option("url", JDBC_URL_ORIGEN) \
    .option("dbtable", "Cotizador.Clientes") \
    .option("user", DB_USER).option("password", DB_PASS).load()

# 2. Guardar crudo en Delta Lake local
# (Si la tabla ya existe, la sobreescribe con la foto del día)
df_cotizaciones_raw.write.format("delta").mode("overwrite").save("/home/jovyan/work/datalake/bronze/cotizaciones")
df_clientes_raw.write.format("delta").mode("overwrite").save("/home/jovyan/work/datalake/bronze/clientes")

# ==========================================
# CAPA SILVER (Limpieza y Dimensiones)
# ==========================================
print("Transformando datos a Capa Silver...")
df_cotizaciones_bronze = spark.read.format("delta").load("/home/jovyan/work/datalake/bronze/cotizaciones")
df_clientes_bronze = spark.read.format("delta").load("/home/jovyan/work/datalake/bronze/clientes")

# Limpieza: Filtrar solo las cotizaciones aprobadas o rechazadas para el DWH
df_silver_cotizaciones = df_cotizaciones_bronze.filter(col("estado").isin("APROBADA", "RECHAZADA"))

# Generar Dim_Tiempo
df_dim_tiempo = df_silver_cotizaciones.select("fecha_creacion") \
    .withColumn("fecha", col("fecha_creacion").cast("date")) \
    .dropDuplicates(["fecha"]) \
    .withColumn("id_tiempo_dim", (year("fecha") * 10000 + month("fecha") * 100 + dayofmonth("fecha")).cast("int")) \
    .withColumn("anio", year("fecha")) \
    .withColumn("trimestre", quarter("fecha")) \
    .withColumn("mes", month("fecha")) \
    .withColumn("dia", dayofmonth("fecha"))

# Generar Dim_Cliente (Clasificando en rangos)
# Lógica simplificada para el ejemplo
df_dim_cliente = df_clientes_bronze.select(
    col("id_cliente").alias("id_cliente_origen"),
    col("ingresos_mensuales"),
    col("score_crediticio")
).withColumn("rango_ingresos", 
    # En un entorno real se usan condicionales de PySpark (when().otherwise())
    col("ingresos_mensuales").cast("string")
).withColumn("rango_score", 
    col("score_crediticio").cast("string")
)

# Guardar en Silver
df_silver_cotizaciones.write.format("delta").mode("overwrite").save("/home/jovyan/work/datalake/silver/cotizaciones")

# ==========================================
# CAPA GOLD (Poblar Modelo Estrella en SQL)
# ==========================================
print("Cargando Modelo Estrella a Capa Gold (Base de Datos)...")

# Insertar Dim_Tiempo en SQL Server (Data Mart)
df_dim_tiempo.select("id_tiempo_dim", "fecha", "anio", "trimestre", "mes", "dia") \
    .write.format("jdbc") \
    .option("url", JDBC_URL_DESTINO) \
    .option("dbtable", "Estrella.Dim_Tiempo") \
    .option("user", DB_USER).option("password", DB_PASS) \
    .mode("append").save() # En real mode("ignore") para no duplicar

# Para la Fact_Cotizaciones, se cruza Silver con las dimensiones para obtener los IDs
# y se guarda en Estrella.Fact_Cotizaciones.
print("ETL Medallion completado con éxito.")
