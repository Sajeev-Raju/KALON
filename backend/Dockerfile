# Build stage
FROM maven:3.9-eclipse-temurin-17 AS build
WORKDIR /app

# Copy pom and download dependencies
COPY pom.xml ./
RUN mvn dependency:go-offline -B

# Copy source and build
COPY src ./src
RUN mvn package -DskipTests -B

# Run stage
FROM eclipse-temurin:17-jre
WORKDIR /app

# Create non-root user (use 1001 to avoid conflict with base image GID 1000)
RUN groupadd --gid 1001 app && useradd --uid 1001 --gid app --no-create-home app

# Copy JAR from build
COPY --from=build /app/target/kalon-backend-*.jar app.jar

# Uploads directory (persisted via volume in compose)
RUN mkdir -p uploads && chown -R app:app uploads

USER app
EXPOSE 8080

ENTRYPOINT ["java", "-jar", "app.jar"]
