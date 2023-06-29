FROM openjdk:17
ARG JAR_FILE=target/*.jar
EXPOSE 8086
COPY ${JAR_FILE} orders.jar
ENTRYPOINT ["java","-jar","/orders.jar"]
