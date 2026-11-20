FROM eclipse-temurin:17-jdk-jammy AS build
WORKDIR /workspace

RUN apt-get update \
    && apt-get install -y --no-install-recommends curl unzip \
    && rm -rf /var/lib/apt/lists/*

COPY .mvn/wrapper/maven-wrapper.properties .mvn/wrapper/maven-wrapper.properties
COPY mvnw pom.xml ./
COPY src/main/ src/main/

RUN chmod +x mvnw \
    && ./mvnw -B -ntp -Dmaven.test.skip=true package \
    && set -- target/*.jar \
    && [ "$#" -eq 1 ] \
    && mv "$1" /workspace/app.jar

FROM eclipse-temurin:17-jre-jammy
WORKDIR /app
COPY --from=build /workspace/app.jar /app/app.jar

USER 10001:10001
EXPOSE 8080
ENTRYPOINT ["java", "-jar", "/app/app.jar"]
