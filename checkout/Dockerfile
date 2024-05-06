FROM ghcr.io/steadybit/zulu-openjdk:main
ARG JAR_FILE=target/*.jar
EXPOSE 8085
COPY ${JAR_FILE} checkout.jar
COPY target/licenses licenses
ENTRYPOINT ["java","-XX:+EnableDynamicAgentLoading","-jar","/checkout.jar"]
