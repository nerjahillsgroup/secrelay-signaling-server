fun Application.module() {
    install(ForwardedHeaders)

    // --- INICIO DE LA MODIFICACIÓN: HABILITAR PING/PONG ---
    // Se añade la configuración de ping/pong al plugin de WebSockets.
    // El servidor enviará un 'ping' cada 15 segundos.
    // Si no recibe un 'pong' de vuelta en 30 segundos, Ktor considerará la
    // conexión como muerta y la cerrará, resolviendo el problema de las
    // conexiones zombis de forma automática y robusta.
    install(WebSockets) {
        pingPeriod = java.time.Duration.ofSeconds(15)
        timeout = java.time.Duration.ofSeconds(30)
    }
    // --- FIN DE LA MODIFICACIÓN ---

    configureSerialization()
    configureRouting()
}