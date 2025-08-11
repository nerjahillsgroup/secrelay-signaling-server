package com.example.plugins

import io.ktor.server.application.*
import io.ktor.server.websocket.*
import kotlin.time.Duration.Companion.seconds // USAMOS LA IMPORTACIÓN DE KOTLIN

fun Application.configureSockets() {
    install(WebSockets) {
        pingPeriod = 15.seconds // USAMOS LA SINTAXIS DE KOTLIN
        timeout = 15.seconds  // USAMOS LA SINTAXIS DE KOTLIN
        maxFrameSize = Long.MAX_VALUE
        masking = false
    }
}