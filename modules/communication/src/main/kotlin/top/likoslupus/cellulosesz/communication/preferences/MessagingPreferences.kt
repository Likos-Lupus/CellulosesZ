package top.likoslupus.cellulosesz.communication.preferences

import top.likoslupus.cellulosesz.communication.config.ReplyMode
import java.util.*

/**
 * Per-player communication preferences. Owned independently of any global "UserData" object.
 * `replyMode == null` means "use the current server default".
 */
internal data class MessagingPreferences(
    val receivePrivateMessages: Boolean = true,
    val ignoredPlayerIds: Set<UUID> = emptySet(),
    val replyMode: ReplyMode? = null,
)
