package com.example.plugins

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
                                println("--> Relaying message from ${message.sender} to ${message.recipient}")
                                recipientSession.send(text)
                            } else {
                                println("--> Recipient ${message.recipient} not found. Message from ${message.sender} dropped.")
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