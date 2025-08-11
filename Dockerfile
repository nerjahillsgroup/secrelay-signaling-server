FROM openjdk:17-jdk-slim
WORKDIR /app
COPY . .
RUN chmod +x ./gradlew
RUN ./gradlew shadowJar --no-daemon
EXPOSE 8080
CMD ["java", "-jar", "build/libs/secrelay-signaling-server-0.0.1-all.jar"]