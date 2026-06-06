FROM eclipse-temurin:21-jdk-alpine AS builder
WORKDIR /workspace

COPY mvnw .
COPY .mvn .mvn
COPY pom.xml .

RUN ./mvnw dependency:go-offline -q

COPY src src
RUN ./mvnw clean package -DskipTests -q

FROM eclipse-temurin:21-jre-alpine AS layertools
WORKDIR /workspace
COPY --from=builder /workspace/target/*.jar app.jar
RUN java -Djarmode=layertools -jar app.jar extract

FROM eclipse-temurin:21-jre-alpine AS runtime

RUN addgroup -S appgroup && adduser -S appuser -G appgroup
USER appuser

WORKDIR /app

COPY --from=layertools /workspace/dependencies/ ./
COPY --from=layertools /workspace/spring-boot-loader/ ./
COPY --from=layertools /workspace/snapshot-dependencies/ ./
COPY --from=layertools /workspace/application/ ./

ENV APP_HEALTH_PORT=9090

EXPOSE 9090

HEALTHCHECK --interval=30s --timeout=3s --start-period=30s --retries=3 \
  CMD wget -qO- http://localhost:${APP_HEALTH_PORT}/actuator/health || exit 1

ENTRYPOINT ["java", \
  "-XX:+UseContainerSupport", \
  "-XX:MaxRAMPercentage=75.0", \
  "org.springframework.boot.loader.launch.JarLauncher"]
