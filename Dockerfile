FROM eclipse-temurin:21-jdk-jammy

WORKDIR /app

# Instalar dependências do sistema
RUN apt-get update && apt-get install -y \
    python3 \
    python3-pip \
    python3-venv \
    ffmpeg \
    && rm -rf /var/lib/apt/lists/*

# Criar e ativar ambiente virtual Python
RUN python3 -m venv /venv
ENV PATH="/venv/bin:$PATH"

# Instalar yt-dlp dentro do ambiente virtual
RUN /venv/bin/pip install --no-cache-dir yt-dlp

# Copiar arquivos do Maven
COPY mvnw .
COPY .mvn .mvn
COPY pom.xml .

# Baixar dependências
RUN ./mvnw dependency:go-offline

# Copiar código fonte
COPY src ./src

# Construir aplicação
RUN ./mvnw clean package -DskipTests

# Volume para downloads
VOLUME /app/downloads

EXPOSE 8080

ENTRYPOINT ["java", "-jar", "target/download-link-0.0.1-SNAPSHOT.jar"]