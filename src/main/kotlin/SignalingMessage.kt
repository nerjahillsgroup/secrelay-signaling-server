import kotlinx.serialization.Serializable

@Serializable
data class SignalingMessage(
    val type: String,
    val sender: String,
    val receiver: String,
    val payload: String
)