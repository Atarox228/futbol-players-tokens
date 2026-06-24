# Multi-stage build
# Stage 1: Build
FROM gradle:8.14-jdk21 as builder
WORKDIR /app
COPY build.gradle settings.gradle ./
COPY src ./src
RUN gradle build -x test

# Stage 2: Runtime with Selenium and Chrome
FROM selenium/standalone-chrome:latest

# Install Java and update packages
USER root
RUN apt-get update && apt-get install -y \
    openjdk-21-jre-headless \
    ca-certificates \
    tzdata \
    && rm -rf /var/lib/apt/lists/*

# Set timezone to Buenos Aires (UTC-3)
ENV TZ=America/Argentina/Buenos_Aires
RUN ln -snf /usr/share/zoneinfo/$TZ /etc/localtime && echo $TZ > /etc/timezone

WORKDIR /app
COPY --from=builder /app/build/libs/*.jar app.jar

# Set environment variables
ENV CHROMIUM_BIN=/usr/bin/chromium
ENV CHROMEDRIVER_BIN=/usr/bin/chromedriver
ENV DISPLAY=:99

# Create startup script
RUN echo '#!/bin/bash\n\
/opt/bin/entry_point.sh &\n\
sleep 5\n\
java -jar app.jar --server.port=${PORT:-8080}\n\
' > /app/start.sh && chmod +x /app/start.sh

EXPOSE 8080 4444
ENTRYPOINT ["/app/start.sh"]
