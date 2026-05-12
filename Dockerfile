# Build stage
FROM eclipse-temurin:21-jdk-alpine AS build
WORKDIR /build

# Copy Maven files and source code
COPY backend/pom.xml .
COPY backend/src ./src

# Install Maven and build
RUN apk add --no-cache maven && mvn clean package -DskipTests -q

# Runtime stage
FROM eclipse-temurin:21-jre-alpine
WORKDIR /app

# Create data directory for SQLite
RUN mkdir -p /data

# Copy the built JAR
COPY --from=build /build/target/xcstring-editor-1.0.0.jar app.jar

# Copy frontend static files (build context is project root)
COPY public ./public

# Expose the default Spring Boot port
EXPOSE 8080

# Environment variables
ENV DB_DRIVER=sqlite \
    DB_SQLITE_PATH=/data/database.sqlite \
    SERVER_PORT=8080

# Run the application
ENTRYPOINT ["java", "-jar", "app.jar"]
