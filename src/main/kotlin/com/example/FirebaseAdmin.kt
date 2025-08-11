package com.example

import com.google.auth.oauth2.GoogleCredentials
import com.google.firebase.FirebaseApp
import com.google.firebase.FirebaseOptions
import java.io.FileInputStream
import java.io.ByteArrayInputStream

object FirebaseAdmin {

    fun initializeFCM() {
        try {
            // --- CORRECCIÓN --- Lógica para funcionar tanto en Render como localmente.
            // 1. Intentamos obtener la credencial desde la variable de entorno (para Render).
            val credentialsJson = System.getenv("GOOGLE_CREDENTIALS")

            val credentialsStream = if (credentialsJson != null) {
                // Estamos en Render. La variable contiene el JSON.
                println("--> Leyendo credenciales de GOOGLE_CREDENTIALS (entorno de Render).")
                ByteArrayInputStream(credentialsJson.toByteArray(Charsets.UTF_8))
            } else {
                // No estamos en Render. Buscamos el archivo local (para pruebas).
                println("--> GOOGLE_CREDENTIALS no encontrada. Buscando archivo local firebase-service-account.json.")
                ClassLoader.getSystemResourceAsStream("firebase-service-account.json")
                    ?: throw IllegalStateException("Archivo firebase-service-account.json no encontrado en resources.")
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