# Multi-stage build
# Stage 1: Build
FROM gradle:8.14-jdk21 as builder
WORKDIR /app
COPY build.gradle settings.gradle ./
COPY src ./src
RUN gradle build -x test

# Stage 2: Runtime (lightweight JRE, no Chrome/Selenium Grid needed — BrightData handles the browser)
FROM eclipse-temurin:21-jre

# Set timezone to Buenos Aires (UTC-3)
ENV TZ=America/Argentina/Buenos_Aires
RUN ln -snf /usr/share/zoneinfo/$TZ /etc/localtime && echo $TZ > /etc/timezone

WORKDIR /app
COPY --from=builder /app/build/libs/*.jar app.jar

# Accept FOOTBALL_DATA_API_TOKEN as build arg and persist as env var
ARG FOOTBALL_DATA_API_TOKEN
ENV FOOTBALL_DATA_API_TOKEN=$FOOTBALL_DATA_API_TOKEN

EXPOSE 8080
ENTRYPOINT ["java", "-jar", "app.jar", "--server.port=${PORT:-8080}"]
