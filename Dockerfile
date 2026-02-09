FROM maven:3.9.6-eclipse-temurin-21-alpine AS builder

WORKDIR /app

COPY . .

RUN mvn clean package -DskipTests

RUN ls -la target/

FROM eclipse-temurin:21-jre-alpine
WORKDIR /app

COPY --from=builder /app/target/EMBank*.jar app.jar

EXPOSE 8080
ENTRYPOINT ["java", "-jar", "app.jar"]