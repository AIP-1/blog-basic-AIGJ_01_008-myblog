# 1단계: 빌드
FROM maven:3.9-eclipse-temurin-17 AS build
WORKDIR /app
COPY pom.xml .
RUN mvn -q dependency:go-offline
COPY src ./src
RUN mvn -q package -DskipTests

# 2단계: 실행
# 코드 실행기가 javac 를 쓰기 때문에 JRE 가 아닌 JDK 이미지를 사용한다
FROM eclipse-temurin:17-jdk
WORKDIR /app
RUN useradd --create-home blog && mkdir -p /app/data && chown blog /app/data
USER blog
COPY --from=build /app/target/java-blog-*.jar app.jar
ENV JAVA_OPTS="-Xmx256m -XX:+UseSerialGC"
EXPOSE 8080
CMD ["sh", "-c", "java $JAVA_OPTS -jar app.jar"]
