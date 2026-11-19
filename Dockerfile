# Sillón · imagen de dos etapas (Clase 16)
# La versión de Java (17) debe coincidir con <java.version> del pom.xml.

# Etapa 1: compila el .jar con Maven (sin tests: los tests corren en el calificador y en local)
FROM maven:3.9-eclipse-temurin-17 AS compilacion
WORKDIR /app
COPY pom.xml .
RUN mvn -B -q dependency:go-offline
COPY src ./src
RUN mvn -B -DskipTests package

# Etapa 2: solo el JRE y el .jar, imagen más liviana
FROM eclipse-temurin:17-jre
WORKDIR /app
COPY --from=compilacion /app/target/*.jar app.jar
EXPOSE 8080
# Render free tiene 512 MB: la JVM usa como máximo el 75 % de esa memoria
ENTRYPOINT ["java", "-XX:MaxRAMPercentage=75.0", "-jar", "app.jar"]
