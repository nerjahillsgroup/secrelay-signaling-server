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

// --- AÑADIDO --- Modelo de datos para la petición de prueba.
@Serializable
data class TestFcmRequest(val token: String)

fun Application.configureRouting() {
    routing {
        // --- AÑADIDO --- Nueva ruta POST para probar el envío de notificaciones.
        post("/test-fcm") {
            try {
                val request = call.receive<TestFcmRequest>()
                if (request.token.isNotBlank()) {
                    println("--> Recibida petición de prueba para el token: ${request.token}")
                    // Usamos el FCMManager con datos de prueba.
                    FCMManager.sendIncomingCallNotification(
                        recipientFcmToken = request.token,
                        senderPublicKey = "SERVIDOR_DE_PRUEBAS",
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

            connections[myPublicKey] = this
            println("--> User connected: $myPublicKey. Total connections: ${connections.size}")

            try {
                for (frame in incoming) {
                    if (frame is Frame.Text) {
                        val text = frame.readText()
                        try {
                            val message = Json.decodeFromString<SignalingMessage>(text)
                            val recipientSession = connections[message.recipient]

                            if (recipientSession != null) {
                                println("--> Relaying message from ${message.sender} to ${message.recipient}")
                                recipientSession.send(text)
                            } else {
                                if (message.type == "OFFER" && message.recipientFcmToken != null) {
                                    println("--> Recipient ${message.recipient} not found. Sending FCM notification.")
                                    FCMManager.sendIncomingCallNotification(
                                        recipientFcmToken = message.recipientFcmToken,
                                        senderPublicKey = message.sender,
                                        offerPayload = message.payload
                                    )
                                } else {
                                    println("--> Recipient ${message.recipient} not found and no FCM info. Message dropped.")
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