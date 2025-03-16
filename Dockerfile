FROM eclipse-temurin:21-jdk-jammy

WORKDIR /app

RUN apt-get update && apt-get install -y --no-install-recommends \
    python3 python3-pip python3-venv ffmpeg \
    && rm -rf /var/lib/apt/lists/*

RUN python3 -m venv /venv && \
    /venv/bin/pip install --no-cache-dir yt-dlp

ENV PATH="/venv/bin:$PATH"

COPY mvnw pom.xml ./
COPY .mvn .mvn
RUN ./mvnw dependency:go-offline

COPY src ./src
RUN ./mvnw clean package -DskipTests

VOLUME /app/downloads

EXPOSE 8080

CMD ["java", "-jar", "target/download-link-0.0.1-SNAPSHOT.jar"]
