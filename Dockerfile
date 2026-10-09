# Build the React storefront with Node.js.
FROM node:22-alpine AS frontend-build

WORKDIR /frontend
COPY frontend/package*.json ./
RUN npm ci
COPY frontend/ ./
RUN npm run build

# Run the Java API and serve the compiled React storefront.
FROM eclipse-temurin:17-jdk

# Use /app as the working folder inside the container.
WORKDIR /app

# Copy the Java source code into the container.
COPY src/main/java ./src/main/java

# Compile the Java files into the out folder.
RUN find src/main/java -name '*.java' > sources.txt \
    && javac --add-modules jdk.httpserver -d out @sources.txt

# Copy the production React files built in the first stage.
COPY --from=frontend-build /frontend/dist ./frontend/dist

# Make the app's web port available to Docker.
EXPOSE 8080

# Start the store when the container runs.
CMD ["java", "--add-modules", "jdk.httpserver", "-cp", "out", "com.fieldnote.store.presentation.StoreServer", "8080"]