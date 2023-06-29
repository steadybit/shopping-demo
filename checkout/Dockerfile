FROM openjdk:17
ARG JAR_FILE=target/*.jar
EXPOSE 8085
COPY ${JAR_FILE} app.jar
ENTRYPOINT ["java","-jar","/app.jar"]
