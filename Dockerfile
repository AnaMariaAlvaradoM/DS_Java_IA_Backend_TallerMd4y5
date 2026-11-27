# Etapa 1: Compilación con Maven y JDK 25
FROM maven:3.9.9-eclipse-temurin-17 AS build
WORKDIR /app

# Copiamos primero el pom.xml para aprovechar la caché de dependencias
COPY pom.xml .
RUN mvn dependency:go-offline -B

# Copiamos el código fuente y empaquetamos sin ejecutar tests
COPY src ./src
RUN mvn clean package -DskipTests

# Etapa 2: Ejecución ligera solo con JRE 17
FROM eclipse-temurin:17-jre-alpine
WORKDIR /app

# Copiamos únicamente el .jar generado desde la primera etapa
COPY --from=build /app/target/*.jar app.jar

EXPOSE 8080

ENTRYPOINT ["java", "-jar", "app.jar"]