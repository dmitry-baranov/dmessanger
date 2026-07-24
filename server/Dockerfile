FROM gradle:8.10.2-jdk21 AS build

WORKDIR /workspace

COPY settings.gradle.kts build.gradle.kts ./
COPY server/build.gradle.kts server/build.gradle.kts
COPY server/src server/src

RUN gradle :server:test :server:bootJar --no-daemon

FROM eclipse-temurin:21-jre

WORKDIR /app

COPY --from=build /workspace/server/build/libs/*.jar /app/app.jar

EXPOSE 8080

ENTRYPOINT ["java", "-jar", "/app/app.jar"]
