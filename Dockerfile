FROM eclipse-temurin:17-jdk
LABEL authors="river"

WORKDIR /app

ENV SERVER_PORT=9999 \
    FILE_PATH=/fileServer

COPY target/file-server-1.0.0.jar app.jar

EXPOSE ${SERVER_PORT}

ENTRYPOINT ["java", "-jar", "app.jar"]
