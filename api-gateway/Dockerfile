FROM eclipse-temurin:21-jdk-alpine AS builder

WORKDIR /app
COPY gradle.properties ./
COPY gradlew settings.gradle build.gradle ./
COPY gradle ./gradle
COPY buildSrc ./buildSrc
COPY common-lib ./common-lib
COPY api-gateway ./api-gateway

RUN ./gradlew :api-gateway:bootJar -x test --no-daemon

FROM eclipse-temurin:21-jre-alpine
WORKDIR /app
COPY --from=builder /app/api-gateway/build/libs/*.jar app.jar
EXPOSE 8080
ENTRYPOINT ["java", "-jar", "app.jar"]
