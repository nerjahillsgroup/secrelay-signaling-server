package com.example.plugins

import com.example.AuthMessageTypes
import com.example.FCMManager
import com.example.SignalingMessage
import com.google.crypto.tink.BinaryKeysetReader
import com.google.crypto.tink.KeysetHandle
import com.google.crypto.tink.PublicKeyVerify
import io.ktor.server.application.*
import io.ktor.server.routing.*
import io.ktor.server.websocket.*
import io.ktor.websocket.*
import kotlinx.coroutines.withTimeoutOrNull
import kotlinx.serialization.Serializable
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import java.util.*
import java.util.concurrent.ConcurrentHashMap

val connections = ConcurrentHashMap<String, WebSocketSession>()
val jsonParser = Json { ignoreUnknownKeys = true }

@Serializable
data class TestFcmRequest(val token: String)

fun verifyWithTink(publicKeyB64: String, data: String, signatureB64: String): Boolean {
    return try {
        val keyBytes = Base64.getDecoder().decode(publicKeyB64)
        val signatureBytes = Base64.getDecoder().decode(signatureB64)
        val dataBytes = data.toByteArray(Charsets.UTF_8)

        val publicKeyHandle = KeysetHandle.readNoSecret(BinaryKeysetReader.withBytes(keyBytes))
        val verifier = publicKeyHandle.getPrimitive(PublicKeyVerify::class.java)
        verifier.verify(signatureBytes, dataBytes)
        true
    } catch (e: Exception) {
        false
    }
}

fun Application.configureRouting() {
    routing {
        webSocket("/ws/signal/{publicKey}") {
            val myPublicKey = call.parameters["publicKey"]
            if (myPublicKey == null) {
                close(CloseReason(CloseReason.Codes.PROTOCOL_ERROR, "Public key is required"))
                return@webSocket
            }

            // Evitar que un usuario se conecte dos veces o expulse a otro
            if (connections.containsKey(myPublicKey)) {
                close(CloseReason(CloseReason.Codes.VIOLATED_POLICY, "User already connected"))
                return@webSocket
            }

            var isAuthenticated = false
            try {
                // --- FASE 1: Lógica de Autenticación (ya implementada) ---
                val challenge = UUID.randomUUID().toString()
                val challengeRequest = SignalingMessage(type = AuthMessageTypes.CHALLENGE_REQUEST, challenge = challenge)
                send(Frame.Text(jsonParser.encodeToString(challengeRequest)))

                val responseFrame = withTimeoutOrNull(10_000) { incoming.receive() } as? Frame.Text
                if (responseFrame == null) {
                    close(CloseReason(CloseReason.Codes.VIOLATED_POLICY, "Authentication timeout"))
                    return@webSocket
                }

                val responseMessage = jsonParser.decodeFromString<SignalingMessage>(responseFrame.readText())

                if (responseMessage.type == AuthMessageTypes.CHALLENGE_RESPONSE && responseMessage.signature != null) {
                    if (verifyWithTink(myPublicKey, challenge, responseMessage.signature)) {
                        isAuthenticated = true
                        connections[myPublicKey] = this // Añadir a la lista de conexiones activas
                        val authSuccess = SignalingMessage(type = AuthMessageTypes.AUTH_SUCCESS)
                        send(Frame.Text(jsonParser.encodeToString(authSuccess)))
                    }
                }

                if (!isAuthenticated) {
                    close(CloseReason(CloseReason.Codes.VIOLATED_POLICY, "Authentication failed"))
                    return@webSocket
                }

                // --- INICIO DE LA MODIFICACIÓN: Lógica de Retransmisión ---
                for (frame in incoming) {
                    if (frame is Frame.Text) {
                        try {
                            val messageText = frame.readText()
                            val message = jsonParser.decodeFromString<SignalingMessage>(messageText)

                            val recipientKey = message.recipient
                            if (recipientKey.isNullOrBlank()) continue // Ignorar mensajes sin destinatario

                            val recipientSession = connections[recipientKey]

                            // Caso 1: El destinatario está conectado. Retransmitir el mensaje.
                            if (recipientSession != null && recipientSession.isActive) {
                                recipientSession.send(Frame.Text(messageText))
                            }
                            // Caso 2: El destinatario NO está conectado.
                            else {
                                // Solo se envían notificaciones push para la solicitud de llamada inicial.
                                if (message.type == "CALL_REQUEST") {
                                    if (!message.recipientFcmToken.isNullOrBlank() &&
                                        !message.senderHash.isNullOrBlank() &&
                                        !message.recipientHash.isNullOrBlank()) {

                                        // NOTA: 'FirebaseAdmin.sendCallNotification' es un placeholder.
                                        // Debes reemplazarlo con tu propia implementación de envío de FCM.
                                        FirebaseAdmin.sendCallNotification(
                                            token = message.recipientFcmToken,
                                            senderHash = message.senderHash,
                                            recipientHash = message.recipientHash
                                        )
                                    }
                                }
                            }
                        } catch (e: Exception) {
                            // Ignorar mensajes malformados para no tumbar la conexión
                            // Puedes añadir un log aquí si lo necesitas
                        }
                    }
                }
                // --- FIN DE LA MODIFICACIÓN ---

            } catch (e: Exception) {
                // Captura errores de conexión, timeouts, etc.
            } finally {
                // Asegurarse de que el usuario se elimina de la lista al desconectarse
                if (myPublicKey != null) {
                    connections.remove(myPublicKey)
                }
            }
        }
    }
}