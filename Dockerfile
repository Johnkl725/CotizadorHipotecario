# ==========================================
# Etapa 1: Construcción (Builder)
# ==========================================
FROM eclipse-temurin:17-jdk-alpine AS builder
WORKDIR /app

# Copiar archivos de configuración de Gradle
COPY gradlew .
COPY gradle gradle
COPY build.gradle settings.gradle ./

# Descargar dependencias (Aprovechamos la caché de Docker)
RUN ./gradlew dependencies --no-daemon || true

# Copiar el código fuente
COPY src src

# Compilar el proyecto empaquetándolo en un JAR (omitiendo tests para mayor velocidad)
RUN ./gradlew bootJar --no-daemon -x test

# ==========================================
# Etapa 2: Producción (Runner)
# ==========================================
FROM eclipse-temurin:17-jre-alpine
WORKDIR /app

# Por seguridad, creamos un usuario sin privilegios de root para correr la aplicación
RUN addgroup -S springuser && adduser -S springuser -G springuser
USER springuser:springuser

# Variables de entorno por defecto (se sobrescribirán en Railway/Render)
ENV PORT=8080
ENV SPRING_PROFILES_ACTIVE=prod

# Copiar solo el JAR final compilado desde la etapa anterior
COPY --from=builder /app/build/libs/*.jar app.jar

# Exponer el puerto
EXPOSE ${PORT}

# Iniciar la aplicación Java
ENTRYPOINT ["java", "-Xms128m", "-Xmx512m", "-Dserver.port=${PORT}", "-jar", "app.jar"]
