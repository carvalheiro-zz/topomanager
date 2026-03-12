# Estágio 1: Build (Maven + JDK 21)
FROM maven:3.9.6-eclipse-temurin-21-alpine AS build
WORKDIR /app

# Cache de dependências para build mais rápido
COPY pom.xml .
RUN mvn dependency:go-offline [cite: 2]

# Compilação do JAR
COPY src ./src
RUN mvn clean package -DskipTests

# Estágio 2: Runtime (Apenas JRE para produção)
FROM eclipse-temurin:21-jre-alpine
WORKDIR /app

# Segurança: Execução com usuário não-root
RUN addgroup -S spring && adduser -S spring -G spring
RUN mkdir /app/temp && chown spring:spring /app/temp
USER spring:spring

# Copia o JAR do estágio de build com o nome definido no seu pom.xml
COPY --from=build /app/target/topomanager.jar app.jar

EXPOSE 8080

# Injeta os parâmetros de memória definidos no docker-compose
ENTRYPOINT ["sh", "-c", "java $JAVA_OPTS -jar app.jar"]