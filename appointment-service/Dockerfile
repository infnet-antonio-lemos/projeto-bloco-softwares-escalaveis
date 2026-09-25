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

# Quebra o jar em camadas por volatilidade (dependências → loader → aplicação).
# Sem isso, trocar uma linha de código reescreve um único blob de ~60 MB; com as
# camadas separadas, só a camada da aplicação (alguns KB) muda a cada deploy —
# o que encurta push, pull e rollout no Kubernetes.
RUN java -Djarmode=tools -jar target/appointment-service-0.0.1-SNAPSHOT.jar extract --layers --destination extracted

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

COPY --from=build --chown=spring:spring /app/extracted/dependencies/ ./
COPY --from=build --chown=spring:spring /app/extracted/spring-boot-loader/ ./
COPY --from=build --chown=spring:spring /app/extracted/snapshot-dependencies/ ./
# O jar extraído procura suas dependências em ./lib (Class-Path do manifesto),
# que é exatamente onde a camada 'dependencies' acima as colocou.
COPY --from=build --chown=spring:spring /app/extracted/application/appointment-service-0.0.1-SNAPSHOT.jar ./app.jar

USER 1001:1001
EXPOSE 8082

# MaxRAMPercentage em vez de -Xmx fixo: a JVM passa a respeitar o limite de
# memória do container (cgroup), que no Kubernetes vem do resources.limits.
ENV JAVA_OPTS="-XX:MaxRAMPercentage=75.0 -XX:+ExitOnOutOfMemoryError"

HEALTHCHECK --interval=10s --timeout=3s --start-period=60s --retries=10 \
  CMD wget -q -O- http://127.0.0.1:8082/actuator/health | grep -q UP || exit 1

ENTRYPOINT ["sh", "-c", "exec java $JAVA_OPTS -jar app.jar"]
