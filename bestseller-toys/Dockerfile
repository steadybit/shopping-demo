FROM openjdk:17
ARG JAR_FILE=target/*.jar
EXPOSE 8081
COPY ${JAR_FILE} bestseller-toys.jar
COPY target/licenses licenses
ENTRYPOINT ["java","-jar","/bestseller-toys.jar"]
