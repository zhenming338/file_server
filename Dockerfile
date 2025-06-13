FROM openjdk:17-jdk-slim
LABEL authors="river"
WORKDIR /app
ENV SERVER_PORT=9999\
    FILE_PATH=/fileServer
COPY target/file-server-0.0.1-SNAPSHOT.jar app.jar
EXPOSE ${SERVER_PORT}
ENTRYPOINT ["java", "-jar","app.jar"]