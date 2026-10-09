# Build the SPA with a compatible Node runtime.
FROM node:22-alpine AS frontend
WORKDIR /web
COPY frontend-angular/package.json frontend-angular/package-lock.json ./
RUN npm ci
COPY frontend-angular/ ./
RUN npm run build

FROM eclipse-temurin:17-jdk-alpine AS builder
WORKDIR /app
COPY gradlew settings.gradle build.gradle ./
COPY gradle gradle
RUN chmod +x gradlew
COPY src src
COPY --from=frontend /web/dist/frontend-angular/browser frontend-angular/dist/frontend-angular/browser
RUN ./gradlew bootJar --no-daemon -x buildFrontend

FROM eclipse-temurin:17-jre-alpine
WORKDIR /app
RUN addgroup -S springuser && adduser -S springuser -G springuser
COPY --from=builder --chown=springuser:springuser /app/build/libs/NewCotizador-0.0.1-SNAPSHOT.jar app.jar
USER springuser:springuser
ENV PORT=8080
ENV SPRING_PROFILES_ACTIVE=prod
EXPOSE 8080
# Spring reads PORT from application.properties; JSON entrypoints do not expand shell variables.
ENTRYPOINT ["java", "-Xms128m", "-Xmx512m", "-jar", "app.jar"]
