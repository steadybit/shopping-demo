FROM openjdk:17
ARG JAR_FILE=target/*.jar
EXPOSE 8082
COPY ${JAR_FILE} fashion-bestseller.jar
COPY target/licenses licenses
ENTRYPOINT ["java","-jar","/fashion-bestseller.jar"]
