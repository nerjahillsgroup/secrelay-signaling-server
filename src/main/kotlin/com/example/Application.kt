package com.example

import com.example.plugins.configureRouting
import com.example.plugins.configureSerialization
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
    
    // --- CONFIGURACIÓN DE WEBSOCKETS CORRECTA Y VERIFICADA ---
    // Esto configura Ktor para que gestione los pings que envía el cliente.
    install(WebSockets) {
        // Si el servidor no recibe NINGÚN frame (Ping o Texto) en 30 segundos,
        // cerrará la conexión automáticamente. Como el cliente envía pings cada 15s,
        // la conexión se mantendrá viva mientras la app esté abierta.
        timeout = Duration.ofSeconds(30)
        maxFrameSize = Long.MAX_VALUE
        masking = false
    }
    
    configureSerialization()
    configureRouting()
}