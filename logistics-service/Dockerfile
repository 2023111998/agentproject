FROM eclipse-temurin:17-jre-alpine
WORKDIR /app
COPY logistics-service/target/*.jar app.jar
EXPOSE 8003
ENTRYPOINT ["java", "-jar", "app.jar"]
