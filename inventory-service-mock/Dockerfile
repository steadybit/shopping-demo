FROM azul/zulu-openjdk-debian:21

RUN apt-get -qq update && \
    apt-get -qq -y upgrade && \
    apt-get -qq -y autoremove && \
    rm -rf /var/lib/apt/lists/*
ARG JAR_FILE=target/*.jar
EXPOSE 8084
COPY ${JAR_FILE} inventory.jar
COPY target/licenses licenses
ENTRYPOINT ["java","-XX:+EnableDynamicAgentLoading","-jar","/inventory.jar"]
