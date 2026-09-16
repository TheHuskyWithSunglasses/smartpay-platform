FROM maven:3.9-eclipse-temurin-21 AS build
WORKDIR /app
COPY pom.xml .
COPY api-gateway ./api-gateway
COPY merchant-service ./merchant-service
COPY payment-service ./payment-service
COPY notification-service ./notification-service
RUN mvn clean package -DskipTests -pl payment-service --also-make

FROM eclipse-temurin:21-jre-alpine
WORKDIR /app
COPY --from=build /app/payment-service/target/payment-service*.jar app.jar
EXPOSE 8080
ENTRYPOINT ["java", "-jar", "app.jar"]