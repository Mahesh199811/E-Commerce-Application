# Build the API and bundle its MySQL JDBC driver.
FROM maven:3.9.9-eclipse-temurin-17 AS build

WORKDIR /app
COPY pom.xml ./pom.xml
COPY src/main/java ./src/main/java
RUN mvn -q package

# Run the API on a smaller Java 17 runtime image.
FROM eclipse-temurin:17-jre
WORKDIR /app
COPY --from=build /app/target/store-app.jar ./store-app.jar

EXPOSE 8080
CMD ["java", "--add-modules", "jdk.httpserver", "-jar", "store-app.jar", "8080"]