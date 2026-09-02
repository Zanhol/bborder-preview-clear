# bborder 预览版 backend runtime image (pre-built jar, no Maven step)
# jar 由宿主 mvn package 预编译（backend/target/family-meal-backend-1.0.0.jar）
FROM eclipse-temurin:17-jre-alpine

WORKDIR /app
ENV TZ=Asia/Shanghai JAVA_OPTS="-Xmx512m"

COPY target/family-meal-backend-1.0.0.jar app.jar

EXPOSE 8088
ENTRYPOINT ["sh", "-c", "java $JAVA_OPTS -jar app.jar"]
