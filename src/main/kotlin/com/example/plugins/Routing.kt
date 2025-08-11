package com.example.plugins

import com.example.FCMManager
import com.example.SignalingMessage
import io.ktor.server.application.*
import io.ktor.server.routing.*
import io.ktor.server.websocket.*
import io.ktor.websocket.*
import kotlinx.serialization.json.Json
import java.util.concurrent.ConcurrentHashMap

val connections = ConcurrentHashMap<String, WebSocketSession>()

fun Application.configureRouting() {
    routing {
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
                                // --- CASO 1: Destinatario CONECTADO ---
                                // Simplemente retransmitimos el mensaje.
                                println("--> Relaying message from ${message.sender} to ${message.recipient}")
                                recipientSession.send(text)
                            } else {
                                // --- CASO 2: Destinatario NO CONECTADO ---
                                if (message.type == "OFFER" && message.recipientFcmToken != null) {
                                    // Si es una oferta y tenemos un token, enviamos la notificación "timbre".
                                    println("--> Recipient ${message.recipient} not found. Sending FCM notification.")
                                    FCMManager.sendIncomingCallNotification(
                                        recipientFcmToken = message.recipientFcmToken,
                                        senderPublicKey = message.sender,
                                        offerPayload = message.payload
                                    )
                                } else {
                                    // Si no es una oferta o no hay token, lo descartamos.
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