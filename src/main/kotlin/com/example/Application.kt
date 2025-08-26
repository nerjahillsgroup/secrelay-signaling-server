package com.example

import com.example.plugins.configureRouting
import com.example.plugins.configureSerialization
import com.google.crypto.tink.config.TinkConfig
import io.ktor.server.application.*
import io.ktor.server.engine.*
import io.ktor.server.netty.*
import io.ktor.server.plugins.forwardedheaders.*
import io.ktor.server.websocket.*
import java.time.Duration

fun main() {
    TinkConfig.register()
    FCMManager.initialize()

    embeddedServer(Netty, port = 8080, host = "0.0.0.0", module = Application::module)
        .start(wait = true)
}

fun Application.module() {
    install(ForwardedHeaders)
    
    // --- CORRECCIÓN: Configuración de Ping/Pong movida aquí y corregida ---
    install(WebSockets) {
        pingPeriod = Duration.ofSeconds(15)
        timeout = Duration.ofSeconds(30)
    }
    
    configureSerialization()
    configureRouting()
}