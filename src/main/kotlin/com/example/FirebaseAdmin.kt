package com.example

import com.google.auth.oauth2.GoogleCredentials
import com.google.firebase.FirebaseApp
import com.google.firebase.FirebaseOptions
import java.io.FileInputStream

object FirebaseAdmin {

    fun initializeFCM() {
        try {
            // Buscamos el archivo de credenciales en la carpeta de recursos.
            // ClassLoader.getSystemResourceAsStream se asegura de que funcione tanto localmente como dentro del JAR.
            val serviceAccount = ClassLoader.getSystemResourceAsStream("firebase-service-account.json")
                ?: throw IllegalStateException("Archivo firebase-service-account.json no encontrado en resources.")

            val options = FirebaseOptions.builder()
                .setCredentials(GoogleCredentials.fromStream(serviceAccount))
                .build()

            // Solo inicializamos si no hay ya una app por defecto.
            if (FirebaseApp.getApps().isEmpty()) {
                FirebaseApp.initializeApp(options)
                println("--> Firebase Admin SDK inicializado correctamente.")
            }
        } catch (e: Exception) {
            println("--> ERROR: Fallo al inicializar Firebase Admin SDK: ${e.message}")
            // En un caso real, podríamos querer que la aplicación no arranque si Firebase es crítico.
        }
    }
}