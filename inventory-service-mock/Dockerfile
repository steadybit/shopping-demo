FROM --platform=$BUILDPLATFORM azul/zulu-openjdk-debian:25 AS builder
ARG BUILDPLATFORM
ARG TARGETPLATFORM
ARG TARGETARCH
ARG TARGETOS

WORKDIR /application
ARG JAR_FILE=target/*.jar
COPY ${JAR_FILE} application.jar
RUN java -Djarmode=layertools -jar application.jar extract

FROM  azul/zulu-openjdk-debian:25
WORKDIR /application

RUN apt-get update && \
    apt-get upgrade -y && \
    apt-get clean

COPY --from=builder /application/dependencies/ ./
COPY --from=builder /application/spring-boot-loader/ ./
COPY --from=builder /application/snapshot-dependencies/ ./
COPY --from=builder /application/application/ ./
COPY target/licenses /licenses

EXPOSE 8084

ENTRYPOINT ["java", "-XX:+EnableDynamicAgentLoading", "-XX:MaxRAMPercentage=75.0", "org.springframework.boot.loader.launch.JarLauncher"]