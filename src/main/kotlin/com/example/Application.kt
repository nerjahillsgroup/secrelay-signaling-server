package com.example

import com.example.plugins.configureRouting
import com.example.plugins.configureSerialization
import com.example.plugins.configureSockets
import io.ktor.server.application.*
import io.ktor.server.engine.*
import io.ktor.server.netty.*
import io.ktor.server.plugins.forwardedheaders.*
import io.ktor.server.websocket.*

fun main() {
    FirebaseAdmin.initializeFCM()

    embeddedServer(Netty, port = 8080, host = "0.0.0.0", module = Application::module)
        .start(wait = true)
}

fun Application.module() {
    install(ForwardedHeaders)
    install(WebSockets) // Instalación simple, sin configuración de timeout aquí.
    
    configureSerialization()
    // La llamada a configureSockets() es redundante si solo instala WebSockets, pero no hace daño.
    // Si tienes un archivo Sockets.kt, asegúrate de que solo contenga 'fun Application.configureSockets() { install(WebSockets) }'
    configureSockets() 
    configureRouting()
}