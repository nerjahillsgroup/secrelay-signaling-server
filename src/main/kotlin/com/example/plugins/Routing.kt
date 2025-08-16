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
                                // Si el destinatario está online, le retransmitimos CUALQUIER tipo de mensaje.
                                println("--> Retransmitiendo ${message.type} de ${message.sender.take(10)} a ${message.recipient.take(10)}")
                                recipientSession.send(Frame.Text(text))
                            } else {
                                // Si está offline, ahora actuamos si es un OFFER o un CHAT_REQUEST.
                                // --- INICIO DE LA MODIFICACIÓN ---
                                if ((message.type == "OFFER" || message.type == "CHAT_REQUEST") && message.recipientFcmToken != null && message.senderHash != null && message.recipientHash != null) {
                                    println("--> Destinatario de ${message.type} offline. Enviando notificación FCM ofuscada.")
                                    FCMManager.sendIncomingCallNotification(
                                        recipientFcmToken = message.recipientFcmToken,
                                        senderHash = message.senderHash,
                                        recipientHash = message.recipientHash,
                                        offerPayload = message.payload // Reutilizamos este campo para la "invitación"
                                    )
                                } else {
                                    // Si es cualquier otro tipo de mensaje (ANSWER, ICE, o nuestro RELAY_MSG)
                                    // y el usuario no está online, no podemos hacer nada. El mensaje se descarta.
                                    println("--> Destinatario ${message.recipient.take(10)} no encontrado para mensaje tipo ${message.type}. Mensaje descartado.")
                                }
                                // --- FIN DE LA MODIFICACIÓN ---
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