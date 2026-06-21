# Stage 1: Build Stage
FROM maven:3.9.6-eclipse-temurin-21-alpine AS build
WORKDIR /app

# Copy pom.xml and download dependencies first to utilize Docker layer caching
COPY pom.xml .
RUN mvn dependency:go-offline -B

# Copy the rest of the source code and compile
COPY src ./src
RUN mvn clean package -DskipTests

# Stage 2: Runtime Stage
FROM eclipse-temurin:21-jre-alpine
WORKDIR /app

# Copy jar from build stage
COPY --from=build /app/target/*.jar app.jar

# Expose server port
EXPOSE 8080

# Run the spring boot application
ENTRYPOINT ["java", "-jar", "app.jar"]
