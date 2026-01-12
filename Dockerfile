# Multi-stage build for optimized image size

# Stage 1: Build
FROM gradle:8.5-jdk17 AS build
WORKDIR /app

# Copy gradle files first for better caching
COPY build.gradle settings.gradle ./
COPY gradle ./gradle

# Download dependencies (cached if gradle files don't change)
RUN gradle dependencies --no-daemon || true

# Copy source code
COPY src ./src

# Build the application
RUN gradle bootJar --no-daemon

# Stage 2: Runtime
FROM eclipse-temurin:17-jre
WORKDIR /app

# Install curl for health checks
USER root
RUN apt-get update && apt-get install -y curl && rm -rf /var/lib/apt/lists/*

# Create non-root user for security
RUN groupadd -r spring && useradd -r -g spring spring
USER spring:spring

# Copy jar from build stage
COPY --from=build /app/build/libs/*.jar app.jar

# Expose port
EXPOSE 8080

# Run the application
ENTRYPOINT ["java", "-Djava.security.egd=file:/dev/./urandom", "-jar", "app.jar"]
