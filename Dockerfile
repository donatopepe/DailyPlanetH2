# syntax=docker/dockerfile:1
#
# Immagine multi-architettura (linux/amd64 + linux/arm64): le immagini di base
# eclipse-temurin e postgres pubblicano entrambe le architetture.
#
# Costruzione:
#   docker build -t dailyplaneth2:1.0.0 .
# Multi-architettura (richiede buildx + QEMU):
#   docker buildx build --platform linux/amd64,linux/arm64 -t dailyplaneth2:1.0.0 .

# ---------- fase di build ----------
FROM eclipse-temurin:17-jdk-jammy AS build

WORKDIR /workspace

# Prima il wrapper e i file di build: i layer delle dipendenze restano in cache
# finché build.gradle non cambia.
COPY gradlew gradlew
COPY gradle gradle
COPY build.gradle settings.gradle ./
RUN chmod +x gradlew \
    && ./gradlew --no-daemon dependencies --configuration runtimeClasspath --quiet

COPY src src
RUN ./gradlew --no-daemon bootJar

# ---------- immagine di esecuzione ----------
FROM eclipse-temurin:17-jre-jammy

# curl serve solo all'HEALTHCHECK; le liste apt vengono rimosse subito
RUN apt-get update \
    && apt-get install -y --no-install-recommends curl \
    && rm -rf /var/lib/apt/lists/*

# l'applicazione non gira come root
RUN groupadd --system --gid 1001 app \
    && useradd --system --uid 1001 --gid app --home-dir /app app

WORKDIR /app
COPY --from=build --chown=app:app /workspace/build/libs/*.jar /app/app.jar

USER app
EXPOSE 8080

# Il profilo di default in container è "prod": richiede DB_URL, DB_USERNAME,
# DB_PASSWORD e DAILYPLANET_ADMIN_PASSWORD (validati all'avvio, senza default segreti).
ENV SPRING_PROFILES_ACTIVE=prod \
    JAVA_OPTS=""

HEALTHCHECK --interval=30s --timeout=5s --start-period=60s --retries=3 \
    CMD curl -fsS "http://localhost:8080/actuator/health" || exit 1

ENTRYPOINT ["sh", "-c", "exec java $JAVA_OPTS -jar /app/app.jar"]
