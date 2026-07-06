FROM eclipse-temurin:17-jre-alpine
WORKDIR /app
COPY product-service/target/*.jar app.jar
EXPOSE 8002
ENTRYPOINT ["java", "-jar", "app.jar"]
