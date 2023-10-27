FROM openjdk:17
ARG JAR_FILE=target/*.jar
EXPOSE 8084
COPY ${JAR_FILE} inventory.jar
COPY target/licenses licenses
ENTRYPOINT ["java","-jar","/inventory.jar"]
