# ============================================================
# Containerfile (Backend) — Multi-Stage OCI-Image
# Build: podman build -t lwe-backend -f Containerfile .
# ============================================================

# --- Stage 1: Build mit Maven + JDK ---
FROM eclipse-temurin:21-jdk-alpine AS build
WORKDIR /app
COPY pom.xml mvnw ./
COPY .mvn .mvn
RUN ./mvnw dependency:resolve -q
COPY src src
RUN ./mvnw package -DskipTests -q

# --- Stage 2: Runtime mit JRE only ---
FROM eclipse-temurin:21-jre-alpine
RUN addgroup -S lwe && adduser -S lwe -G lwe
WORKDIR /app
COPY --from=build /app/target/*.jar app.jar
USER lwe
EXPOSE 8080
ENTRYPOINT ["java", "-jar", "app.jar"]
