# syntax=docker/dockerfile:1

# ---- build ----
FROM maven:3.9-eclipse-temurin-21 AS builder
WORKDIR /build

# Dependencies resolve in their own layer, so a source-only change does not re-download them.
COPY pom.xml ./
RUN mvn -B -ntp dependency:go-offline

COPY src ./src
RUN mvn -B -ntp clean package -DskipTests

# ---- runtime ----
FROM eclipse-temurin:21-jre-alpine

# ffmpeg is baked in rather than installed at every container start: the old compose
# entrypoint ran `apk add ffmpeg` on each boot, which made startup depend on the network,
# pulled an unpinned version, and needed root inside the container.
RUN apk add --no-cache ffmpeg wget \
 && addgroup -S yogida && adduser -S -G yogida yogida

WORKDIR /app
COPY --from=builder /build/target/*.jar app.jar
RUN chown -R yogida:yogida /app

USER yogida
EXPOSE 8080

# JAVA_TOOL_OPTIONS is honoured because the JVM reads it itself; the shell form also lets
# JAVA_OPTS through, which the exec form would have silently ignored.
ENV JAVA_TOOL_OPTIONS="-XX:+UseContainerSupport -XX:MaxRAMPercentage=75.0"
ENTRYPOINT ["sh", "-c", "exec java $JAVA_OPTS -jar /app/app.jar"]
