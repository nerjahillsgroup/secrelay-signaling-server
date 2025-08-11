package com.example

import com.google.firebase.messaging.AndroidConfig
import com.google.firebase.messaging.AndroidNotification
import com.google.firebase.messaging.FirebaseMessaging
import com.google.firebase.messaging.Message
import com.google.firebase.messaging.Notification

object FCMManager {
    fun sendIncomingCallNotification(recipientFcmToken: String, senderPublicKey: String, offerPayload: String) {
        // --- CONFIGURACIÓN ESPECÍFICA Y EXPLÍCITA PARA ANDROID ---
        // Esto imita el comportamiento de alta prioridad de la consola de Firebase.
        val androidConfig = AndroidConfig.builder()
            .setPriority(AndroidConfig.Priority.HIGH) // Prioridad máxima
            .setTtl(0) // Tiempo de vida 0 segundos, entrega inmediata o nunca
            .setNotification(
                AndroidNotification.builder()
                    .setTitle("Llamada Entrante")
                    .setBody("Estás recibiendo una llamada en Secrelay.")
                    .setChannelId("INCOMING_CALL_CHANNEL_ID") // Es CRUCIAL que coincida con el canal en la app
                    .build()
            )
            .build()

        val message = Message.builder()
            // El bloque .setNotification() es opcional si se usa AndroidConfig, pero lo dejamos por compatibilidad.
            .setNotification(Notification.builder()
                .setTitle("Llamada Entrante")
                .setBody("Estás recibiendo una llamada en Secrelay.")
                .build())
            .putData("type", "INCOMING_CALL")
            .putData("senderPublicKey", senderPublicKey)
            .putData("offerPayload", offerPayload)
            .setToken(recipientFcmToken)
            .setAndroidConfig(androidConfig) // <-- LA PARTE MÁS IMPORTANTE
            .build()

        try {
            val response = FirebaseMessaging.getInstance().send(message)
            println("--> Notificación FCM de alta prioridad enviada. Message ID: $response")
        } catch (e: Exception) {
            println("--> ERROR al enviar notificación FCM: ${e.message}")
        }
    }
}