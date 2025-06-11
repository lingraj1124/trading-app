# --- Stage 1: Build the application ---
FROM maven:3.9.5-eclipse-temurin-17 AS build

WORKDIR /app

# Copy only pom.xml first to leverage Docker cache
COPY pom.xml .

# Download dependencies (cached unless pom.xml changes)
RUN mvn dependency:go-offline

# Now copy source code
COPY src ./src

# Build the project
RUN mvn -B clean package -DskipTests || { echo 'Maven build failed'; exit 1; }

# --- Stage 2: Run the application ---
FROM eclipse-temurin:17-jdk

WORKDIR /app

# Copy the built JAR file from the build stage
COPY --from=build /app/target/tradingview-webhook-0.0.1-SNAPSHOT.jar app.jar

# Expose the application port
EXPOSE 8080

# Run the JAR file
CMD ["java", "-Dserver.port=8080", "-jar", "app.jar"]
