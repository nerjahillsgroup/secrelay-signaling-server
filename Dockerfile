FROM openjdk:17-jdk-slim
WORKDIR /app
COPY . .
RUN chmod +x ./gradlew
RUN ./gradlew shadowJar --no-daemon
EXPOSE 8080
# --- CORRECCIÓN --- Se ajusta el nombre del JAR para que coincida con el artefacto generado por la tarea shadowJar.
CMD ["java", "-jar", "build/libs/secrelay-signaling-server-all.jar"]