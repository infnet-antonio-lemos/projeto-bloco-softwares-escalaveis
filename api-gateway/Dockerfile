# =============================================================================
# Build
# =============================================================================
FROM maven:3.9-eclipse-temurin-21 AS build
WORKDIR /app

# pom.xml sozinho primeiro: enquanto as dependências não mudarem, esta camada é
# reaproveitada do cache e o build não volta a baixar o repositório inteiro.
COPY pom.xml .
RUN mvn -B -q dependency:go-offline

COPY src ./src
RUN mvn -B -q clean package -DskipTests

# =============================================================================
# Runtime
# =============================================================================
FROM eclipse-temurin:21-jre-alpine
WORKDIR /app

# Usuário sem privilégios: um comprometimento da aplicação não vira root no nó.
# Exigido pelo securityContext dos manifests do Kubernetes (runAsNonRoot).
# UID numérico e explícito: o Kubernetes precisa saber o id para validar
# runAsNonRoot. Com um nome de usuário (USER spring), o kubelet não consegue
# resolver o uid a partir da imagem e recusa o pod.
RUN addgroup -g 1001 -S spring && adduser -u 1001 -S -G spring spring

# Jar único (fat jar), em vez de extraído em camadas por volatilidade: o ganho das
# camadas é encurtar push/pull de um registry remoto a cada deploy, cenário que este
# projeto não exercita. Ver docs/tp5.md §5.1.
COPY --from=build --chown=spring:spring /app/target/api-gateway-0.0.1-SNAPSHOT.jar ./app.jar

USER 1001:1001
EXPOSE 8080

# MaxRAMPercentage em vez de -Xmx fixo: a JVM passa a respeitar o limite de
# memória do container (cgroup), que no Kubernetes vem do resources.limits.
ENV JAVA_OPTS="-XX:MaxRAMPercentage=75.0 -XX:+ExitOnOutOfMemoryError"

ENTRYPOINT ["sh", "-c", "exec java $JAVA_OPTS -jar app.jar"]
