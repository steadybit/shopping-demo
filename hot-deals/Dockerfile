FROM openjdk:17
ARG JAR_FILE=target/*.jar
EXPOSE 8083
COPY ${JAR_FILE} hot-deals.jar
COPY target/licenses licenses
ENTRYPOINT ["java","-jar","/hot-deals.jar"]
