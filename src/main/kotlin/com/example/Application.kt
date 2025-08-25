package com.example

import com.example.plugins.configureRouting
import com.example.plugins.configureSerialization
import com.google.crypto.tink.config.TinkConfig
import io.ktor.server.application.*
import io.ktor.server.engine.*
import io.ktor.server.netty.*
import io.ktor.server.plugins.forwardedheaders.*
import io.ktor.server.websocket.*

fun main() {
    TinkConfig.register()
    FirebaseAdmin.initializeFCM()

    embeddedServer(Netty, port = 8080, host = "0.0.0.0", module = Application::module)
        .start(wait = true)
}

fun Application.module() {
    install(ForwardedHeaders)
    install(WebSockets) 
    
    configureSerialization()
    configureRouting()
}