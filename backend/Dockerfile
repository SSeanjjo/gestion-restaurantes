# ---- Etapa 1: compilar con Maven ----
FROM maven:3.9-eclipse-temurin-21 AS build
WORKDIR /app

# Descargar dependencias primero para aprovechar la caché de capas
COPY pom.xml .
RUN mvn -q -B dependency:go-offline

COPY src ./src
RUN mvn -q -B package -DskipTests

# ---- Etapa 2: imagen de ejecución ----
FROM eclipse-temurin:21-jre
WORKDIR /app

RUN useradd --system --uid 1001 comanda
COPY --from=build /app/target/comanda-backend-*.jar app.jar
USER comanda

EXPOSE 8080
ENTRYPOINT ["java", "-XX:MaxRAMPercentage=75", "-jar", "/app/app.jar"]
