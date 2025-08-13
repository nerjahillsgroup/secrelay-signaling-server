package com.example

import com.google.firebase.messaging.AndroidConfig
import com.google.firebase.messaging.AndroidNotification
import com.google.firebase.messaging.FirebaseMessaging
import com.google.firebase.messaging.Message
import com.google.firebase.messaging.Notification

object FCMManager {
    // --- CAMBIO: La firma de la función ahora usa los hashes para la ofuscación ---
    fun sendIncomingCallNotification(recipientFcmToken: String, senderHash: String, recipientHash: String, offerPayload: String) {
        val androidConfig = AndroidConfig.builder()
            .setPriority(AndroidConfig.Priority.HIGH)
            .setTtl(0)
            .setNotification(
                AndroidNotification.builder()
                    .setTitle("Lamada Entrante")
                    .setBody("Estás recibiendo una llamada en Secrelay.")
                    .setChannelId("INCOMING_CALL_CHANNEL_ID")
                    .build()
            )
            .build()

        val message = Message.builder()
            .setNotification(Notification.builder()
                .setTitle("Lamada Entrante")
                .setBody("Estás recibiendo una llamada en Secrelay.")
                .build())
            // --- CAMBIO: El payload ahora contiene los hashes con claves claras y consistentes ---
            .putData("type", "INCOMING_CALL")
            .putData("offerPayload", offerPayload)
            .putData("call_sender_hash", senderHash)
            .putData("call_recipient_hash", recipientHash)
            .setToken(recipientFcmToken)
            .setAndroidConfig(androidConfig)
            .build()

        try {
            val response = FirebaseMessaging.getInstance().send(message)
            println("--> Notificación FCM de alta prioridad enviada. Message ID: $response")
        } catch (e: Exception) {
            println("--> ERROR al enviar notificación FCM: ${e.message}")
        }
    }
}