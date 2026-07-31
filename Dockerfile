FROM eclipse-temurin:21-jdk AS build
WORKDIR /build
COPY .mvn/ .mvn/
COPY mvnw pom.xml ./
COPY src/ src/
COPY frontend/ frontend/
RUN ./mvnw package -DskipTests

FROM eclipse-temurin:21-jre-alpine
WORKDIR /app
COPY --from=build /build/target/*.jar app.jar
EXPOSE 8080
ENTRYPOINT ["java", "-jar", "app.jar"]
