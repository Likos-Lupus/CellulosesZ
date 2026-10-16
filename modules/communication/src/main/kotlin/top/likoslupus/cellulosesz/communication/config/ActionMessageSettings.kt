package top.likoslupus.cellulosesz.communication.config

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/** Who receives `/me` action messages. */
@Serializable
public enum class ActionAudience {

    /** Players in the sender's current dimension. */
    @SerialName("same_dimension")
    SAME_DIMENSION,

    /** Every online player. */
    @SerialName("server")
    SERVER,

}

/** `/me <action>` behavior: audience, length cap and rate limit. */
@Serializable
public data class ActionMessageSettings(
    public val enabled: Boolean = true,
    public val audience: ActionAudience = ActionAudience.SAME_DIMENSION,
    public val maximumLength: Int = 256,
    public val messagesPerMinute: Int = 8,
)
