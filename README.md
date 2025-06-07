[JAVA_BADGE]:https://img.shields.io/badge/java-%23ED8B00.svg?style=for-the-badge&logo=openjdk&logoColor=white
[SPRING_BADGE]:https://img.shields.io/badge/spring-%2382B54B.svg?style=for-the-badge&logo=spring&logoColor=white
[DOCKER_BADGE]:https://img.shields.io/badge/docker-%230db7ed.svg?style=for-the-badge&logo=docker&logoColor=white

<br>

<div align="center">
  <h1 style="font-weight;">YouTube Downloader 🎵</h1>
</div>

![spring][SPRING_BADGE]
![java][JAVA_BADGE]
![docker][DOCKER_BADGE]

<br>

<p align="center">
  <b>Projeto para download de vídeos do YouTube.</b>
</p>

## 🚀 Começando

Este projeto é uma API desenvolvida com Spring Boot que permite baixar vídeos do YouTube a partir de uma URL fornecida pelo usuário. A aplicação é empacotada com Docker para facilitar a execução em qualquer ambiente.

## ⚙️ Tecnologias

- **Linguagem**: Java 21
- **Framework**: Spring Boot (Web, Lombok, Validation)
- **Ferramenta de construção**: Maven
- **Contêinerização**: Docker

## 🔄 Clonando

Clone o projeto usando HTTPS:
```
https://github.com/notAvoiid/download-link.git
```

Ou, se preferir usar SSH:
```
git@github.com:notAvoiid/download-link.git
```

## 🟢 Executando o projeto
```bash
# 1. Navegue até o diretório do projeto:
cd download-link

# 2. Faça o build da aplicação:
./mvnw clean install -DskipTests

# 3. Inicialize o projeto utilizando o docker-compose.yml:
docker compose up --build -d

```
## 📋 Exemplos de Uso no Terminal

**Baixar música pelo Link:**
```bash
curl -X POST -H "Content-Type: application/json" \
-d '{"url": "https://www.youtube.com/watch?v=VIDEO_ID"}' \
http://localhost:8080/api/download
```

<strong>OBS:</strong> A música <strong>não será baixada diretamente para seus arquivos locais</strong>. Ele será armazenado em um <strong>arquivo temporário da aplicação</strong>.
Para realizar o download no seu computador, utilize o <a href="https://github.com/notAvoiid/download-link-frontend" target="_blank">Front-end</a>.
## Front-end

Acesse o github do front-end por meio deste link para utilizar a API de forma completa: <a href="https://github.com/notAvoiid/download-link-frontend" target="_blank">Front-end</a>.

## 📄 Documentação

1. Certifique-se de que o projeto está rodando localmente.
2. Navegue até `http://localhost:8080/swagger-ui/index.html#/` no seu navegador ou clique aqui segurando CTRL: [Swagger](http://localhost:8080/swagger-ui/index.html#/)  

## 📫 Contribuição

Para me ajudar a melhorar o projeto ou me ajudar a melhorar:

1. Clone: `git clone https://github.com/notAvoiid/download-link.git` ou `git clone git@github.com:notAvoiid/download-link.git`
2. Criando sua própria feature: `git checkout -b feature/NAME`
3. Siga os padrões de commit.
4. Abra um Pull Request explicando o problema resolvido ou a feature implementada. Prints com detalhes são importantes!
