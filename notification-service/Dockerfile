FROM eclipse-temurin:21-jdk-alpine AS builder

WORKDIR /app
COPY gradle.properties ./
COPY gradlew settings.gradle build.gradle ./
COPY gradle ./gradle
COPY buildSrc ./buildSrc
COPY common-lib ./common-lib
COPY notification-service ./notification-service

RUN ./gradlew :notification-service:bootJar -x test --no-daemon

FROM eclipse-temurin:21-jre-alpine
WORKDIR /app
COPY --from=builder /app/notification-service/build/libs/*.jar app.jar
EXPOSE 8084
ENTRYPOINT ["java", "-jar", "app.jar"]
