package com.example

import com.google.firebase.messaging.FirebaseMessaging
import com.google.firebase.messaging.Message
import com.google.firebase.messaging.Notification

object FCMManager {

    /**
     * Envía una notificación push a un dispositivo a través de FCM para alertarle de una llamada entrante.
     * @param recipientFcmToken El token del dispositivo que debe recibir la notificación.
     * @param senderPublicKey La clave pública del usuario que origina la llamada.
     * @param offerPayload El SDP de la oferta de WebRTC, que el cliente necesitará para responder.
     */
    fun sendIncomingCallNotification(recipientFcmToken: String, senderPublicKey: String, offerPayload: String) {
        // Construimos la notificación visible para el usuario.
        // Mantenemos el cuerpo del mensaje genérico por privacidad.
        val notification = Notification.builder()
            .setTitle("Llamada Entrante")
            .setBody("Estás recibiendo una llamada en Secrelay.")
            .build()

        // Construimos el mensaje completo, incluyendo los datos que la app necesitará
        // para procesar la llamada cuando el usuario toque la notificación.
        val message = Message.builder()
            .setNotification(notification)
            .putData("type", "INCOMING_CALL") // Tipo de evento personalizado para que el cliente lo identifique.
            .putData("senderPublicKey", senderPublicKey) // Quién llama.
            .putData("offerPayload", offerPayload) // La oferta SDP para poder contestar.
            .setToken(recipientFcmToken) // A qué dispositivo específico se envía.
            .build()

        try {
            // Enviamos el mensaje usando el SDK de Firebase Admin.
            val response = FirebaseMessaging.getInstance().send(message)
            println("--> Notificación FCM enviada con éxito. Message ID: $response")
        } catch (e: Exception) {
            println("--> ERROR al enviar notificación FCM: ${e.message}")
        }
    }
}