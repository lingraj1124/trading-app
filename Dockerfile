# --- Stage 1: Build the application ---
FROM maven:3.9.5-eclipse-temurin-17 AS build
WORKDIR /app

COPY pom.xml .
COPY src ./src

# Run Maven build with logging enabled
RUN mvn -B clean package -DskipTests

# --- Stage 2: Run the application ---
FROM eclipse-temurin:17-jdk
WORKDIR /app

COPY --from=build /app/target/tradingview-webhook-0.0.1-SNAPSHOT.jar app.jar

EXPOSE 8080
CMD ["java", "-Dserver.port=8080", "-jar", "app.jar"]
