FROM openjdk:17
WORKDIR /app
# 빌드된 Spring Boot JAR 파일을 복사
COPY build/libs/cockple.demo-0.0.1-SNAPSHOT.jar cockple.jar

# JAR 파일 실행
# wait-for-it으로 DB 대기 후 Spring Boot 실행
ENTRYPOINT ["sh", "/app/wait-for-it.sh", "mysql:3306", "--", "java", "-jar", "cockple.jar"]