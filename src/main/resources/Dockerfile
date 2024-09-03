FROM openjdk:17
ARG JAR_FILE=app.jar
COPY ${JAR_FILE} app.jar
EXPOSE 9522
ENTRYPOINT ["java","-jar","app.jar"]