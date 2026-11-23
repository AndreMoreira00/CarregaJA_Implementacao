# Imagem da API para a nuvem. As telas vão dentro do pacote, então um contêiner só serve tudo.

# Etapa 1: compila e empacota
FROM maven:3.9-eclipse-temurin-21 AS construcao
WORKDIR /app
COPY carregaja-api/pom.xml .
RUN mvn -q -B dependency:go-offline
COPY carregaja-api/src src
RUN mvn -q -B package -DskipTests

# Etapa 2: só o Java e o pacote
FROM eclipse-temurin:21-jre
WORKDIR /app
COPY --from=construcao /app/target/*.war app.war
# Usa até 75% da memória do contêiner, que nos planos gratuitos é pequena
ENV JAVA_TOOL_OPTIONS="-XX:MaxRAMPercentage=75"
# O serviço de hospedagem informa a porta na variável PORT; sem ela, vale a 8081
CMD ["sh", "-c", "java -jar app.war --server.port=${PORT:-8081}"]
