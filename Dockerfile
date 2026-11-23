
# Etapa 1: Compilar la aplicación con Maven y Java 17
FROM maven:3.9-eclipse-temurin-17 AS compilacion

WORKDIR /app

COPY pom.xml .
COPY src ./src

RUN mvn -B -DskipTests package

# Etapa 2: Ejecutar la aplicación con Java 17
FROM eclipse-temurin:17-jre

WORKDIR /app

COPY --from=compilacion /app/target/sillon-0.0.1-SNAPSHOT.jar app.jar

EXPOSE 8080

ENTRYPOINT ["java", "-jar", "app.jar"]
