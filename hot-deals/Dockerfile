FROM azul/zulu-openjdk-debian:21 AS builder

WORKDIR /application
ARG JAR_FILE=target/*.jar
COPY ${JAR_FILE} application.jar
RUN java -Djarmode=layertools -jar application.jar extract

FROM gcr.io/distroless/java21-debian12:nonroot
WORKDIR /application

COPY --from=builder /application/dependencies/ ./
COPY --from=builder /application/spring-boot-loader/ ./
COPY --from=builder /application/snapshot-dependencies/ ./
COPY --from=builder /application/application/ ./
COPY target/licenses /licenses

EXPOSE 8083

ENTRYPOINT ["java", "-XX:+EnableDynamicAgentLoading", "-XX:MaxRAMPercentage=75.0", "org.springframework.boot.loader.launch.JarLauncher"]