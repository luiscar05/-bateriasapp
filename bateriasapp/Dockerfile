# Etapa 1: compilar el jar (los tests se corren antes, con `mvn test`)
FROM maven:3.9-eclipse-temurin-21 AS build
WORKDIR /app
COPY pom.xml .
RUN mvn -q dependency:go-offline
COPY src ./src
RUN mvn -q -DskipTests package

# Etapa 2: imagen liviana solo con el JRE y el jar
FROM eclipse-temurin:21-jre
WORKDIR /app
COPY --from=build /app/target/bateriasapp-1.0-SNAPSHOT.jar app.jar
ENV TZ=America/Bogota
EXPOSE 8080
# DB_URL, DB_USER y DB_PASSWORD se pasan como variables de entorno al ejecutar el contenedor
ENTRYPOINT ["java", "-jar", "app.jar"]
