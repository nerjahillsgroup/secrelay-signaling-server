package com.example

import com.google.auth.oauth2.GoogleCredentials
import com.google.firebase.FirebaseApp
import com.google.firebase.FirebaseOptions
import com.google.firebase.messaging.FirebaseMessaging
import com.google.firebase.messaging.Message
import java.io.FileInputStream

object FCMManager {

    fun initialize() {
        try {
            val serviceAccount = FileInputStream("/etc/secrets/GOOGLE_CREDENTIALS")

            val options = FirebaseOptions.builder()
                .setCredentials(GoogleCredentials.fromStream(serviceAccount))
                .build()

            if (FirebaseApp.getApps().isEmpty()) {
                FirebaseApp.initializeApp(options)
            }
        } catch (e: Exception) {
            println("ERROR: No se pudo inicializar Firebase Admin SDK. ${e.message}")
        }
    }

    fun sendCallNotification(token: String, senderHash: String, recipientHash: String) {
        try {
            val message = Message.builder()
                .putData("call_sender_hash", senderHash)
                .putData("call_recipient_hash", recipientHash)
                .setToken(token)
                .build()

            FirebaseMessaging.getInstance().send(message)
        } catch (e: Exception) {
            println("ERROR: No se pudo enviar notificación FCM. ${e.message}")
        }
    }
}