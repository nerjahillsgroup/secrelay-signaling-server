package com.example

import com.google.auth.oauth2.GoogleCredentials
import com.google.firebase.FirebaseApp
import com.google.firebase.FirebaseOptions
import java.io.FileInputStream
import java.io.InputStream
import java.io.File

object FirebaseAdmin {

    fun initializeFCM() {
        try {
            // --- CORRECCIÓN FINAL --- Lógica para funcionar en Render y localmente.
            val renderSecretPath = "/etc/secrets/GOOGLE_CREDENTIALS"
            val localResourcePath = "firebase-service-account.json"

            val credentialsStream: InputStream

            // 1. Verificamos si estamos en el entorno de Render buscando su ruta de secretos.
            if (File(renderSecretPath).exists()) {
                println("--> Entorno de Render detectado. Leyendo credenciales desde $renderSecretPath.")
                credentialsStream = FileInputStream(File(renderSecretPath))
            } else {
                // 2. Si no, asumimos que estamos en un entorno local y buscamos en resources.
                println("--> Entorno local detectado. Buscando archivo '$localResourcePath' en resources.")
                credentialsStream = ClassLoader.getSystemResourceAsStream(localResourcePath)
                    ?: throw IllegalStateException("Archivo '$localResourcePath' no encontrado en resources.")
            }

            val options = FirebaseOptions.builder()
                .setCredentials(GoogleCredentials.fromStream(credentialsStream))
                .build()

            if (FirebaseApp.getApps().isEmpty()) {
                FirebaseApp.initializeApp(options)
                println("--> Firebase Admin SDK inicializado correctamente.")
            }
        } catch (e: Exception) {
            println("--> ERROR: Fallo al inicializar Firebase Admin SDK: ${e.message}")
        }
    }
}