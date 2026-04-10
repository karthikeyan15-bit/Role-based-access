# Use Eclipse Temurin for Java 21 (current standard)
FROM eclipse-temurin:21-jdk

# Set the working directory inside the container
WORKDIR /app

# Copy all source files and resources into the container
COPY . .

# Create the output directory for compiled classes
RUN mkdir -p out/classes

# Compile the Java source files
RUN javac -d out/classes \
    src/main/java/com/college/rbac/*.java \
    src/main/java/com/college/rbac/model/*.java \
    src/main/java/com/college/rbac/service/*.java \
    src/main/java/com/college/rbac/controller/*.java \
    src/main/java/com/college/rbac/util/*.java

# Copy the web resources to the classes directory (so they can be served)
RUN cp -r src/main/resources/web out/classes/web

# Expose port 8080 (the port your app listens on)
EXPOSE 8080

# Run the application
CMD ["java", "-cp", "out/classes", "com.college.rbac.Main"]
