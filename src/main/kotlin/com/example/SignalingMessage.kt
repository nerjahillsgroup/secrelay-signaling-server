package com.example

import kotlinx.serialization.Serializable

@Serializable
data class SignalingMessage(
    val type: String,
    val sender: String,
    val recipient: String,
    val payload: String,
    val recipientFcmToken: String? = null,
    // --- CAMPOS AÑADIDOS PARA OFUSCACIÓN ---
    val senderHash: String? = null,
    val recipientHash: String? = null
)