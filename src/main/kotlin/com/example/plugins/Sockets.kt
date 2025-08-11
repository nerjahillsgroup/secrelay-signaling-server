package com.example.plugins

import io.ktor.server.application.*
import io.ktor.server.websocket.*
// --- CORRECCIÓN --- Se importa la clase Duration de Java en lugar de la de Kotlin.
import java.time.Duration

fun Application.configureSockets() {
    install(WebSockets) {
        // --- CORRECCIÓN --- Se utiliza Duration.ofSeconds() para crear una instancia de java.time.Duration.
        pingPeriod = Duration.ofSeconds(15)
        timeout = Duration.ofSeconds(15)
        maxFrameSize = Long.MAX_VALUE
        masking = false
    }
}