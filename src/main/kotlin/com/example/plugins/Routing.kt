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

            var isAuthenticated = false
            try {
                val challenge = UUID.randomUUID().toString()
                val challengeRequest = SignalingMessage(type = AuthMessageTypes.CHALLENGE_REQUEST, challenge = challenge)
                send(Frame.Text(Json.encodeToString(challengeRequest)))

                val responseFrame = withTimeoutOrNull(10_000) { incoming.receive() } as? Frame.Text ?: throw Exception("Timeout or invalid frame")
                
                val responseMessage = Json.decodeFromString<SignalingMessage>(responseFrame.readText())

                if (responseMessage.type == AuthMessageTypes.CHALLENGE_RESPONSE && responseMessage.signature != null) {
                    if (verifyWithTink(myPublicKey, challenge, responseMessage.signature)) {
                        isAuthenticated = true
                        connections[myPublicKey] = this
                        val authSuccess = SignalingMessage(type = AuthMessageTypes.AUTH_SUCCESS)
                        send(Frame.Text(Json.encodeToString(authSuccess)))
                    }
                }

                if (!isAuthenticated) {
                    close(CloseReason(CloseReason.Codes.VIOLATED_POLICY, "Authentication failed"))
                    return@webSocket
                }

                for (frame in incoming) {
                    // Lógica de retransmisión
                }
            } catch (e: Exception) {
                // Manejo de errores
            } finally {
                connections.remove(myPublicKey)
            }
        }
    }
}