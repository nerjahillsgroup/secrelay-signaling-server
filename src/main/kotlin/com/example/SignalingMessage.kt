package com.example

import kotlinx.serialization.Serializable

@Serializable
data class SignalingMessage(
    val type: String,
    val sender: String? = null,
    val recipient: String? = null,
    val payload: String? = null,
    val recipientFcmToken: String? = null,
    val senderHash: String? = null,
    val recipientHash: String? = null,
    val challenge: String? = null,
    val signature: String? = null
)

object AuthMessageTypes {
    const val CHALLENGE_REQUEST = "challenge_request"
    const val CHALLENGE_RESPONSE = "challenge_response"
    const val AUTH_SUCCESS = "auth_success"
    const val AUTH_FAILED = "auth_failed"
}