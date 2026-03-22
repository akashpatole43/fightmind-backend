# ==========================================
# STAGE 1: Build the Application
# ==========================================
# Use the official Maven image to compile the app. 
# We use Eclipse Temurin JDK 17 as it matches our Spring Boot setup.
FROM maven:3.9.6-eclipse-temurin-17 AS builder

# Set the working directory inside the container
WORKDIR /app

# Copy the pom.xml and source code into the container
COPY pom.xml .
COPY src ./src

# Package the application.
# -DskipTests ensures the Docker build doesn't fail if the database isn't running during build time.
RUN mvn clean package -DskipTests

# ==========================================
# STAGE 2: Create the Lightweight Runtime Image
# ==========================================
# We switch to a much smaller JRE (Java Runtime Environment) image 
# instead of a full JDK. This reduces image size and attack surface.
FROM eclipse-temurin:17-jre-jammy

# Set the working directory
WORKDIR /app

# Expose port 8080 (the default Tomcat port for Spring Boot)
EXPOSE 8080

# Create a non-root user for security (best practice for production containers)
RUN addgroup --system spring && adduser --system --group spring
USER spring:spring

# Copy ONLY the compiled .jar file from the 'builder' stage.
# We discard the source code and Maven dependencies to keep the image tiny.
COPY --from=builder /app/target/fightmind-backend-1.0.0-SNAPSHOT.jar app.jar

# Define the command to start the Spring Boot application.
# We use java -jar to execute the compiled artifact.
# You can override Spring profiles at runtime via environment variables (e.g. SPRING_PROFILES_ACTIVE)
ENTRYPOINT ["java", "-jar", "app.jar"]
