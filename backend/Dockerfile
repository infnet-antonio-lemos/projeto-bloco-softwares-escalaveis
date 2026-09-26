# Build stage
FROM maven:3.9-eclipse-temurin-21 AS build
WORKDIR /app
COPY pom.xml .
RUN mvn -B -q dependency:go-offline

COPY src ./src
RUN mvn -B -q clean package -DskipTests

# =============================================================================
# Runtime
# =============================================================================
FROM eclipse-temurin:21-jre-alpine
WORKDIR /app

RUN addgroup -g 1001 -S spring && adduser -u 1001 -S -G spring spring
COPY --from=build --chown=spring:spring /app/target/petclinic-0.0.1-SNAPSHOT.jar ./app.jar

USER 1001:1001
EXPOSE 8081
ENV JAVA_OPTS="-XX:MaxRAMPercentage=75.0 -XX:+ExitOnOutOfMemoryError"

ENTRYPOINT ["sh", "-c", "exec java $JAVA_OPTS -jar app.jar"]
