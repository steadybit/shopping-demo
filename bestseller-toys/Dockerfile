FROM azul/zulu-openjdk-debian:21

RUN apt-get -qq update && \
    apt-get -qq -y upgrade && \
    apt-get -qq -y autoremove && \
    rm -rf /var/lib/apt/lists/*
ARG JAR_FILE=target/*.jar
EXPOSE 8081
COPY ${JAR_FILE} bestseller-toys.jar
COPY target/licenses licenses
ENTRYPOINT ["java","-XX:+EnableDynamicAgentLoading","-jar","/bestseller-toys.jar"]
