FROM eclipse-temurin:17-jdk AS build
WORKDIR /app

COPY pom.xml .
COPY src ./src

# Install Maven and build
RUN apt-get update && \
    apt-get install -y --no-install-recommends maven && \
    rm -rf /var/lib/apt/lists/* && \
    mvn -B package -DskipTests && \
    mv target/*.jar app.jar

FROM eclipse-temurin:17-jre
WORKDIR /app

COPY --from=build /app/app.jar .

EXPOSE 8080

ENTRYPOINT ["java", "-jar", "app.jar"]
