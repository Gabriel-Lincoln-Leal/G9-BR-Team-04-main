# Etapa 1: compilar e testar
FROM eclipse-temurin:21-jdk AS build

WORKDIR /workspace

COPY .mvn/ .mvn/
COPY mvnw pom.xml ./
RUN chmod +x mvnw

COPY src/ src/
RUN ./mvnw --batch-mode clean verify

# Etapa 2: executar
FROM eclipse-temurin:21-jre

WORKDIR /app

RUN groupadd --system techmind \
    && useradd --system --gid techmind techmind \
    && mkdir -p /app/logs \
    && chown -R techmind:techmind /app

COPY --from=build --chown=techmind:techmind \
    /workspace/target/techmind-0.0.1-SNAPSHOT.jar /app/techmind.jar

USER techmind

ENV SERVER_ADDRESS=0.0.0.0

EXPOSE 8080

ENTRYPOINT ["java", "-Xms64m", "-Xmx256m", "-XX:+UseSerialGC", "-XX:MaxMetaspaceSize=160m", "-XX:ReservedCodeCacheSize=64m", "-XX:MaxDirectMemorySize=32m", "-XX:+ExitOnOutOfMemoryError", "-jar", "/app/techmind.jar"]