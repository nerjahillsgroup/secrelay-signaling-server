# --- Fase 1: Compilación ---
# Usamos una imagen de OpenJDK 17 para compilar el proyecto.
# Le damos el alias 'builder'.
FROM openjdk:17-jdk-slim as builder

# Establecemos el directorio de trabajo dentro del contenedor.
WORKDIR /app

# Copiamos los archivos de Gradle. Esto se hace primero para aprovechar la caché de Docker.
# Si no cambiamos las dependencias, Docker no necesitará volver a descargarlas en cada build.
COPY build.gradle.kts settings.gradle.kts gradlew ./
COPY gradle ./gradle

# Damos permisos de ejecución al wrapper de Gradle.
RUN chmod +x ./gradlew

# Copiamos el resto del código fuente.
COPY src ./src

# Ejecutamos el comando de compilación de Gradle para crear un "fat jar" que contenga todas las dependencias.
RUN ./gradlew installDist

# --- Fase 2: Ejecución ---
# Usamos una imagen mucho más ligera que solo contiene el entorno de ejecución de Java, no el JDK completo.
FROM openjdk:17-jre-slim

# Establecemos el directorio de trabajo.
WORKDIR /app

# Copiamos ÚNICAMENTE los artefactos compilados desde la fase 'builder'.
COPY --from=builder /app/build/install/secrelay-signaling-server ./

# El puerto que Ktor usará por defecto. Render lo detectará.
EXPOSE 8080

# El comando que se ejecutará cuando el contenedor se inicie.
# Llama al script de lanzamiento generado por Gradle.
CMD ["./bin/secrelay-signaling-server"]