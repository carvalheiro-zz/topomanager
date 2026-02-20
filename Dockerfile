# Estágio 1: Build
FROM maven:3.9.6-eclipse-temurin-21-alpine AS build
WORKDIR /app
# Copia apenas o pom.xml primeiro para aproveitar o cache de dependências do Docker
COPY pom.xml .
RUN mvn dependency:go-offline

COPY src ./src
RUN mvn clean package -DskipTests

# Estágio 2: Runtime
FROM eclipse-temurin:21-jre-alpine
WORKDIR /app

# Adiciona um usuário não-root por segurança (Boa prática para o GitHub/Produção)
RUN addgroup -S spring && adduser -S spring -G spring
USER spring:spring

# Copia apenas o JAR
COPY --from=build /app/target/*.jar app.jar

EXPOSE 8080

# Adicionamos a variável JAVA_OPTS para que o Docker Compose consiga injetar 
# os limites de memória (-Xmx4g) que definimos antes.
ENTRYPOINT ["sh", "-c", "java $JAVA_OPTS -jar app.jar"]