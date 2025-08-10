import io.ktor.server.application.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import io.ktor.server.websocket.*
import io.ktor.websocket.*
import kotlinx.serialization.json.Json
import java.util.concurrent.ConcurrentHashMap

fun Application.configureRouting() {
    val connections = ConcurrentHashMap<String, WebSocketSession>()

    routing {
        get("/") {
            call.respondText("Hello World!")
        }

        webSocket("/ws/signal") {
            var myPublicKey: String? = null
            try {
                val initialMessage = (incoming.receive() as? Frame.Text)?.readText()
                if (initialMessage == null) {
                    close(CloseReason(CloseReason.Codes.PROTOCOL_ERROR, "Se esperaba la clave pública al conectar."))
                    return@webSocket
                }
                myPublicKey = initialMessage
                connections[myPublicKey] = this
                println("Usuario conectado: $myPublicKey")

                for (frame in incoming) {
                    if (frame is Frame.Text) {
                        val text = frame.readText()
                        try {
                            // La clase SignalingMessage está en el paquete por defecto, se encontrará sin import.
                            val message = Json.decodeFromString<SignalingMessage>(text)
                            val receiverConnection = connections[message.receiver]
                            if (receiverConnection != null) {
                                println("Redirigiendo mensaje de ${message.sender} a ${message.receiver}")
                                receiverConnection.send(text)
                            } else {
                                println("Destinatario ${message.receiver} no encontrado.")
                            }
                        } catch (e: Exception) {
                            println("Error al decodificar JSON: ${e.localizedMessage}")
                        }
                    }
                }
            } catch (e: Exception) {
                println("Error en conexión: ${e.localizedMessage}")
            } finally {
                if (myPublicKey != null) {
                    println("Usuario desconectado: $myPublicKey")
                    connections.remove(myPublicKey)
                }
            }
        }
    }
}