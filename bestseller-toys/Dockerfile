FROM ghcr.io/steadybit/zulu-openjdk:main
ARG JAR_FILE=target/*.jar
EXPOSE 8081
COPY ${JAR_FILE} bestseller-toys.jar
COPY target/licenses licenses
ENTRYPOINT ["java","-XX:+EnableDynamicAgentLoading","-jar","/bestseller-toys.jar"]
