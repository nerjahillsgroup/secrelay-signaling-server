package com.example

import com.example.plugins.configureRouting
import com.example.plugins.configureSerialization
// import com.example.plugins.configureSockets // Eliminamos esta importación, ya no se usa.
import io.ktor.server.application.*
import io.ktor.server.engine.*
import io.ktor.server.netty.*
import io.ktor.server.plugins.forwardedheaders.*
import io.ktor.server.websocket.*
import java.time.Duration

fun main() {
    FirebaseAdmin.initializeFCM()

    embeddedServer(Netty, port = 8080, host = "0.0.0.0", module = Application::module)
        .start(wait = true)
}

fun Application.module() {
    install(ForwardedHeaders)
    
    install(WebSockets) {
        // Esta es la configuración correcta. El servidor enviará pings cada 15s.
        // El cliente responderá automáticamente (es un comportamiento estándar de WebSocket).
        // Si el cliente no responde a tiempo, el timeout lo desconectará.
        pingPeriod = Duration.ofSeconds(15) 
        timeout = Duration.ofSeconds(30) 
        maxFrameSize = Long.MAX_VALUE
        masking = false
    }
    
    configureSerialization()
    // configureSockets() ya no es necesaria.
    configureRouting()
}