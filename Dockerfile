# Usamos una única imagen de OpenJDK 17 que tiene tanto el JDK como el JRE.
FROM openjdk:17-jdk-slim

# Establecemos el directorio de trabajo.
WORKDIR /app

# Copiamos todo el proyecto al contenedor.
COPY . .

# Damos permisos de ejecución al wrapper de Gradle.
RUN chmod +x ./gradlew

# Ejecutamos la tarea de Gradle para crear un "fat jar" con todas las dependencias.
# Esta es la tarea estándar que Ktor espera.
RUN ./gradlew buildFatJar

# Exponemos el puerto estándar de Ktor.
EXPOSE 8080

# El comando para iniciar el servidor. Apunta directamente al JAR compilado.
CMD ["java", "-jar", "build/libs/secrelay-signaling-server-0.0.1-all.jar"]