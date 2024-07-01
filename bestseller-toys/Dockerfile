FROM azul/zulu-openjdk-debian:21 AS builder

WORKDIR /application
ARG JAR_FILE=target/*.jar
COPY ${JAR_FILE} application.jar
RUN java -Djarmode=layertools -jar application.jar extract

FROM azul/zulu-openjdk-debian:21
WORKDIR /application

#install curl for health check in ecs
RUN apt-get update \
      && apt-get install --no-install-recommends -y curl \
      && apt-get -y autoremove \
      && rm -rf /var/lib/apt/lists/*

COPY --from=builder /application/dependencies/ ./
COPY --from=builder /application/spring-boot-loader/ ./
COPY --from=builder /application/snapshot-dependencies/ ./
COPY --from=builder /application/application/ ./
COPY target/licenses /licenses

EXPOSE 8081

ENTRYPOINT ["java", "-XX:+EnableDynamicAgentLoading", "-XX:MaxRAMPercentage=75.0", "org.springframework.boot.loader.launch.JarLauncher"]