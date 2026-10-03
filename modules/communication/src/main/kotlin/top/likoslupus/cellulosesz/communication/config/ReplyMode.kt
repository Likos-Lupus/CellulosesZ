package top.likoslupus.cellulosesz.communication.config

import kotlinx.serialization.Serializable

/** Which remembered counterpart `/reply` resolves to. */
@Serializable
public enum class ReplyMode {

    /** Most recent interaction, incoming or outgoing. */
    LAST_INTERACTION,

    /** Most recent player who messaged you. */
    LAST_INCOMING,

}
