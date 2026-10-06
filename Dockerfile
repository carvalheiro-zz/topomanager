# Estagio 1: Build (Maven + JDK 21)
FROM maven:3.9.6-eclipse-temurin-21-alpine AS build
WORKDIR /app

# Cache de dependencias para build mais rapido
COPY pom.xml .
RUN mvn dependency:go-offline

# Compilacao do JAR
COPY src ./src
RUN mvn clean package -DskipTests

# Estagio 2: Runtime (Apenas JRE para producao)
FROM eclipse-temurin:21-jre-alpine
WORKDIR /app

# Segurança: Execucao com usuario nao-root
RUN addgroup -S spring && adduser -S spring -G spring
RUN mkdir /app/temp && chown spring:spring /app/temp
USER spring:spring

# Copia o JAR compilado
COPY --from=build /app/target/topomanager.jar app.jar

EXPOSE 8080

# Execucao direta com unbuffered I/O direcionado para o console
ENTRYPOINT ["java", "-Djava.io.tmpdir=/app/temp", "-jar", "app.jar"]