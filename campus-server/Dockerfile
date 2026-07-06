FROM eclipse-temurin:17-jre-alpine
WORKDIR /app
COPY campus-server/target/*.jar app.jar
EXPOSE 8000
ENTRYPOINT ["java", "-jar", "app.jar"]
