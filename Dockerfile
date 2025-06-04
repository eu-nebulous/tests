# Stage 1: dependency stage
FROM maven:3.9.4-eclipse-temurin-17 AS dependencies

WORKDIR /app

COPY pom.xml .
RUN mvn dependency:go-offline

#build
FROM maven:3.9.4-eclipse-temurin-17 AS build
WORKDIR /app

COPY --from=dependencies /root/.m2 /root/.m2
COPY . .
RUN mvn clean package -DskipTests

#runntime
FROM maven:3.9.4-eclipse-temurin-17
WORKDIR /app

COPY --from=build /root/.m2 /root/.m2
COPY . .
COPY --from=build /app/target/*.jar ./target/

CMD ["mvn", "-Dtest=eu.nebulouscloud.test.automated.tests.AppDeploymentCloudProvider", "test"]
