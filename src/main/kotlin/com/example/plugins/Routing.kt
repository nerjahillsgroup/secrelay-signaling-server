package com.example.plugins

import com.example.AuthMessageTypes
import com.example.SignalingMessage
import com.google.crypto.tink.BinaryKeysetReader
import com.google.crypto.tink.KeysetHandle
import com.google.crypto.tink.PublicKeyVerify
import io.ktor.server.application.*
import io.ktor.server.routing.*
import io.ktor.server.websocket.*
import io.ktor.websocket.*
// --- INICIO DE LA CORRECCIÓN ---
import io.ktor.websocket.Frame // Import necesario para 'isActive'
// --- FIN DE LA CORRECCIÓN ---
import kotlinx.coroutines.withTimeoutOrNull
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import java.util.*
import java.util.concurrent.ConcurrentHashMap

val connections = ConcurrentHashMap<String, WebSocketSession>()
val jsonParser = Json { ignoreUnknownKeys = true }

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

            if (connections.containsKey(myPublicKey)) {
                close(CloseReason(CloseReason.Codes.VIOLATED_POLICY, "User already connected"))
                return@webSocket
            }

            var isAuthenticated = false
            try {
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
                        connections[myPublicKey] = this
                        val authSuccess = SignalingMessage(type = AuthMessageTypes.AUTH_SUCCESS)
                        send(Frame.Text(jsonParser.encodeToString(authSuccess)))
                    }
                }

                if (!isAuthenticated) {
                    close(CloseReason(CloseReason.Codes.VIOLATED_POLICY, "Authentication failed"))
                    return@webSocket
                }

                for (frame in incoming) {
                    if (frame is Frame.Text) {
                        try {
                            val messageText = frame.readText()
                            val message = jsonParser.decodeFromString<SignalingMessage>(messageText)
                            val recipientKey = message.recipient
                            if (recipientKey.isNullOrBlank()) continue
                            val recipientSession = connections[recipientKey]

                            if (recipientSession != null && recipientSession.isActive) {
                                recipientSession.send(Frame.Text(messageText))
                            } else {
                                if (message.type == "CALL_REQUEST") {
                                    if (!message.recipientFcmToken.isNullOrBlank() && !message.senderHash.isNullOrBlank() && !message.recipientHash.isNullOrBlank()) {
                                        // --- INICIO DE LA CORRECCIÓN ---
                                        // TODO: Reemplazar esta línea con tu implementación real de envío de FCM.
                                        // La línea original 'FirebaseAdmin.sendCallNotification(...)' se comenta para permitir la compilación.
                                        println("Placeholder: Se enviaría una notificación FCM a ${message.recipientFcmToken}")
                                        // --- FIN DE LA CORRECCIÓN ---
                                    }
                                }
                            }
                        } catch (e: Exception) {
                            // Ignorar mensajes malformados
                        }
                    }
                }
            } catch (e: Exception) {
                // Manejo de errores de conexión
            } finally {
                connections.remove(myPublicKey)
            }
        }
    }
}