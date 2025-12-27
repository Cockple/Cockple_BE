FROM eclipse-temurin:17-jdk-jammy

COPY build/libs/cockple.demo-0.0.1-SNAPSHOT.jar app.jar

CMD ["java", "-Dspring.profiles.active=dev", "-jar", "app.jar"]