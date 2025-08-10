# Usamos una única imagen oficial de Gradle que ya contiene OpenJDK 17.
# Esto es más robusto que instalar Java nosotros mismos.
FROM gradle:7.6-jdk17

# Establecemos el directorio de trabajo.
WORKDIR /app

# Copiamos todo el proyecto al contenedor.
COPY . .

# Ejecutamos la tarea de Gradle para crear un "fat jar".
# Se añade --no-daemon para mayor compatibilidad en entornos de CI/CD.
RUN gradle buildFatJar --no-daemon

# Exponemos el puerto estándar de Ktor.
EXPOSE 8080

# El comando para iniciar el servidor.
CMD ["java", "-jar", "build/libs/secrelay-signaling-server-0.0.1-all.jar"]