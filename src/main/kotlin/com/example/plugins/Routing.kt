package com.example.plugins

import com.example.FCMManager
import com.example.SignalingMessage
import io.ktor.server.application.*
import io.ktor.server.request.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import io.ktor.server.websocket.*
import io.ktor.websocket.*
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import java.util.concurrent.ConcurrentHashMap

val connections = ConcurrentHashMap<String, WebSocketSession>()

val jsonParser = Json { ignoreUnknownKeys = true }

@Serializable
data class TestFcmRequest(val token: String)

fun Application.configureRouting() {
    routing {
        post("/test-fcm") {
            try {
                val request = call.receive<TestFcmRequest>()
                if (request.token.isNotBlank()) {
                    println("--> Recibida petición de prueba para el token: ${request.token}")
                    FCMManager.sendIncomingCallNotification(
                        recipientFcmToken = request.token,
                        senderHash = "HASH_SERVIDOR_DE_PRUEBAS",
                        recipientHash = "HASH_SERVIDOR_DE_PRUEBAS",
                        offerPayload = "ESTO_ES_UNA_PRUEBA"
                    )
                    call.respondText("Petición de notificación de prueba enviada al token: ${request.token}")
                } else {
                    call.respondText("Error: El token no puede estar vacío.")
                }
            } catch (e: Exception) {
                call.respondText("Error en el servidor: ${e.message}")
            }
        }

        webSocket("/ws/signal/{publicKey}") {
            val myPublicKey = call.parameters["publicKey"]
            if (myPublicKey == null) {
                close(CloseReason(CloseReason.Codes.PROTOCOL_ERROR, "Public key is required in URL"))
                return@webSocket
            }

            if (connections.containsKey(myPublicKey)) {
                close(CloseReason(CloseReason.Codes.VIOLATED_POLICY, "User already connected."))
                return@webSocket
            }

            connections[myPublicKey] = this
            println("--> User connected: $myPublicKey. Total connections: ${connections.size}")

            try {
                for (frame in incoming) {
                    if (frame is Frame.Text) {
                        val text = frame.readText()
                        try {
                            val message = jsonParser.decodeFromString<SignalingMessage>(text)
                            val recipientSession = connections[message.recipient]

                            if (recipientSession != null) {
                                println("--> Relaying message from ${message.sender} to online user ${message.recipient}")
                                // --- CORRECCIÓN CRÍTICA ---
                                // Usamos Frame.Text para enviar el mensaje, no texto plano.
                                recipientSession.send(Frame.Text(text))
                            } else {
                                if (message.type == "OFFER" && message.recipientFcmToken != null && message.senderHash != null && message.recipientHash != null) {
                                    println("--> Recipient ${message.recipient} not found. Sending OBFUSCATED FCM notification.")
                                    FCMManager.sendIncomingCallNotification(
                                        recipientFcmToken = message.recipientFcmToken,
                                        senderHash = message.senderHash,
                                        recipientHash = message.recipientHash,
                                        offerPayload = message.payload
                                    )
                                } else {
                                    println("--> Recipient ${message.recipient} not found and no valid FCM/Hash info. Message dropped.")
                                }
                            }
                        } catch (e: Exception) {
                            println("--> Error decoding message: ${e.message}")
                        }
                    }
                }
            } catch (e: Exception) {
                println("--> Error for user $myPublicKey: ${e.localizedMessage}")
            } finally {
                println("--> User disconnected: $myPublicKey")
                connections.remove(myPublicKey)
                println("--> Total connections: ${connections.size}")
            }
        }
    }
}