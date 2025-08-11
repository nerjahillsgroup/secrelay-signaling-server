package com.example

import kotlinx.serialization.Serializable

@Serializable
data class SignalingMessage(
    val type: String,
    val sender: String,
    val recipient: String,
    val payload: String,
    // --- AÑADIDO --- Token FCM del destinatario.
    // Es opcional (nullable) porque solo es necesario para el mensaje "OFFER" inicial,
    // que actúa como "timbre" si el destinatario no está conectado.
    val recipientFcmToken: String? = null
)