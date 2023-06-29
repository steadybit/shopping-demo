FROM openjdk:17
ARG JAR_FILE=target/*.jar
EXPOSE 8085
COPY ${JAR_FILE} ckeckout.jar
ENTRYPOINT ["java","-jar","/ckeckout.jar"]
