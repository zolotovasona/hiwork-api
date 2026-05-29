FROM eclipse-temurin:17-jdk-alpine
WORKDIR /app
COPY pom.xml .
RUN mvn dependency:go-offline -B
COPY src ./src
RUN mvn clean package -DskipTests -B
EXPOSE 8080
ENTRYPOINT ["java", "-jar", "target/demo-0.0.1-SNAPSHOT.jar"]