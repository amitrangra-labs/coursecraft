# Multi-stage build for the CourseCraft backend. Used by Render (and reusable on Fly.io later).
FROM maven:3.9-eclipse-temurin-21 AS build
WORKDIR /app
COPY pom.xml .
RUN mvn -q -B dependency:go-offline
COPY src ./src
RUN mvn -q -B package -DskipTests

FROM eclipse-temurin:21-jre
WORKDIR /app
COPY --from=build /app/target/coursecraft-backend-*.jar app.jar
EXPOSE 8080
# MaxRAMPercentage keeps the JVM inside the container memory limit (512 MB on Render free).
ENTRYPOINT ["java", "-XX:MaxRAMPercentage=75", "-jar", "app.jar"]
