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
    
    // --- CONFIGURACIÓN DE WEBSOCKETS DEFINITIVA Y VERIFICADA ---
    // Esto configura Ktor para que gestione correctamente los pings que envía el cliente.
    install(WebSockets) {
        pingPeriod = Duration.ofSeconds(20) // El servidor esperará pings cada 20s. El cliente envía cada 15s.
        timeout = Duration.ofSeconds(40)     // Si no recibe nada (ni ping ni datos) en 40s, cierra la conexión.
        maxFrameSize = Long.MAX_VALUE
        masking = false
    }
    
    configureSerialization()
    configureRouting()
}