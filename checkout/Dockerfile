FROM openjdk:17
ARG JAR_FILE=target/*.jar
EXPOSE 8085
COPY ${JAR_FILE} checkout.jar
COPY target/licenses licenses
ENTRYPOINT ["java","-jar","/checkout.jar"]
