# Usamos una imagen de OpenJDK 17 que sabemos que funciona en Render.
FROM openjdk:17-jdk-slim

# Establecemos el directorio de trabajo.
WORKDIR /app

# Copiamos solo los archivos de configuración de Gradle primero para aprovechar la caché de Docker.
COPY build.gradle.kts settings.gradle.kts gradlew ./
COPY gradle ./gradle

# Damos permisos de ejecución.
RUN chmod +x ./gradlew

# Descargamos las dependencias. Si no cambian, este paso se cacheará en futuros builds.
RUN ./gradlew dependencies

# Copiamos el resto del código fuente.
COPY src ./src

# Usamos la tarea 'installDist' que crea un formato de distribución estándar en build/install.
RUN ./gradlew installDist --no-daemon

# Exponemos el puerto.
EXPOSE 8080

# El comando final llama al script de lanzamiento generado por installDist.
CMD ["build/install/secrelay-signaling-server/bin/secrelay-signaling-server"]