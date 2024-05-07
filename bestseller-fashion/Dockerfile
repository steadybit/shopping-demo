FROM azul/zulu-openjdk-debian:21

RUN apt-get -qq update && \
    apt-get -qq -y upgrade && \
    apt-get -qq -y autoremove && \
    rm -rf /var/lib/apt/lists/*
ARG JAR_FILE=target/*.jar
EXPOSE 8082
COPY ${JAR_FILE} fashion-bestseller.jar
COPY target/licenses licenses
ENTRYPOINT ["java","-XX:+EnableDynamicAgentLoading","-jar","/fashion-bestseller.jar"]
