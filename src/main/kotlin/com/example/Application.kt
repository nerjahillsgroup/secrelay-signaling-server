package com.example

import com.example.plugins.configureRouting
import com.example.plugins.configureSerialization
import com.example.plugins.configureSockets
import io.ktor.server.application.*
import io.ktor.server.engine.*
import io.ktor.server.netty.*
// --- AÑADIDO --- Importación necesaria para el plugin de proxy.
import io.ktor.server.plugins.forwardedheaders.*

fun main() {
    FirebaseAdmin.initializeFCM()

    embeddedServer(Netty, port = 8080, host = "0.0.0.0", module = Application::module)
        .start(wait = true)
}

fun Application.module() {
    // --- AÑADIDO --- Se instala el plugin para manejar cabeceras de proxy.
    // Esto es CRUCIAL para que los WebSockets (wss://) funcionen detrás de un proxy como el de Render.
    install(ForwardedHeaders)
    
    configureSerialization()
    configureSockets()
    configureRouting()
}