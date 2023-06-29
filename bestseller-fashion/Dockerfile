FROM openjdk:17
ARG JAR_FILE=target/*.jar
EXPOSE 8082
COPY ${JAR_FILE} bestseller-fashion.jar
ENTRYPOINT ["java","-jar","/bestseller-fashion.jar"]
