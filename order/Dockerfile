FROM openjdk:21
ARG JAR_FILE=target/*.jar
EXPOSE 8086
COPY ${JAR_FILE} orders.jar
COPY target/licenses licenses
ENTRYPOINT ["java","-jar","/orders.jar","-XX:+EnableDynamicAgentLoading"]
