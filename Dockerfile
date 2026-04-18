# Build stage
FROM maven:3.9.9-eclipse-temurin-21-alpine AS builder

WORKDIR /app

COPY pom.xml .
RUN mvn dependency:go-offline -B

COPY src ./src

RUN mvn -B clean package -DskipTests

FROM eclipse-temurin:21-jre-alpine
WORKDIR /app

RUN addgroup -S spring && adduser -S spring -G spring

COPY --from=builder /app/target/citizen-query-service-*.jar app.jar

USER spring:spring
EXPOSE 8081

ENTRYPOINT ["java", "-XX:+UseContainerSupport", "-Xms128m", "-Xmx2G", "-jar", "app.jar"]
