FROM openjdk:17
ARG JAR_FILE=target/*.jar
EXPOSE 8080
COPY ${JAR_FILE} gateway.jar
COPY target/licenses licenses
ENTRYPOINT ["java","-jar","/gateway.jar"]
