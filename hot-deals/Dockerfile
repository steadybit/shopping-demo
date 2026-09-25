FROM --platform=$BUILDPLATFORM azul/zulu-openjdk-debian:25 AS builder
ARG BUILDPLATFORM
ARG TARGETPLATFORM
ARG TARGETARCH
ARG TARGETOS

WORKDIR /application
ARG JAR_FILE=target/*.jar
COPY ${JAR_FILE} application.jar
RUN java -Djarmode=tools -jar application.jar extract --layers --launcher --destination extracted

FROM azul/zulu-openjdk-debian:25
WORKDIR /application

RUN apt-get update && \
    apt-get upgrade -y && \
    apt-get clean

COPY --from=builder /application/extracted/dependencies/ ./
COPY --from=builder /application/extracted/spring-boot-loader/ ./
COPY --from=builder /application/extracted/snapshot-dependencies/ ./
COPY --from=builder /application/extracted/application/ ./
COPY target/licenses /licenses

EXPOSE 8083

ENTRYPOINT ["java", "-XX:+EnableDynamicAgentLoading", "-XX:MaxRAMPercentage=75.0", "org.springframework.boot.loader.launch.JarLauncher"]