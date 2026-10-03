package top.likoslupus.cellulosesz.communication.messaging

import top.likoslupus.cellulosesz.communication.config.ReplyMode
import java.util.*
import kotlin.time.Duration
import kotlin.time.TimeMark
import kotlin.time.TimeSource

internal data class ReplyTarget(
    val playerId: UUID,
    val updatedAt: TimeMark,
)

/**
 * Session-only reply memory. Confined to the server thread; timing is monotonic because the state
 * never survives a restart. `/reply` resolves through the player's [ReplyMode].
 */
internal class ReplyState(
    private val timeout: () -> Duration?,
    private val timeSource: TimeSource = TimeSource.Monotonic,
) {

    private data class Entry(
        val lastIncoming: ReplyTarget? = null,
        val lastOutgoing: ReplyTarget? = null,
        val lastInteraction: ReplyTarget? = null,
    )

    private val entries = HashMap<UUID, Entry>()

    /** Called only after a private message has actually been delivered to the target. */
    fun recordSuccessfulDelivery(
        senderId: UUID,
        targetId: UUID,
        recipientMode: ReplyMode,
    ) {
        val now = timeSource.markNow()
        val senderTarget = ReplyTarget(targetId, now)
        val senderEntry = entries[senderId] ?: Entry()
        entries[senderId] = senderEntry.copy(
            lastOutgoing = senderTarget,
            lastInteraction = senderTarget,
        )

        val fromSender = ReplyTarget(senderId, now)
        val recipientEntry = entries[targetId] ?: Entry()
        entries[targetId] = recipientEntry.copy(
            lastIncoming = fromSender,
            lastInteraction = when (recipientMode) {
                ReplyMode.LAST_INTERACTION -> fromSender
                ReplyMode.LAST_INCOMING -> recipientEntry.lastInteraction
            },
        )
    }

    fun resolve(playerId: UUID, mode: ReplyMode): ReplyTarget? {
        val entry = entries[playerId]
            ?: return null
        val candidate = when (mode) {
            ReplyMode.LAST_INTERACTION -> entry.lastInteraction
            ReplyMode.LAST_INCOMING -> entry.lastIncoming
        } ?: return null
        return when {
            isExpired(candidate) -> null
            else -> candidate
        }
    }

    fun clear(playerId: UUID) {
        entries.remove(playerId)
        for (key in entries.keys.toList()) {
            val entry = entries[key]
                ?: continue
            entries[key] = entry.copy(
                lastIncoming = entry.lastIncoming?.takeUnless { it.playerId == playerId },
                lastOutgoing = entry.lastOutgoing?.takeUnless { it.playerId == playerId },
                lastInteraction = entry.lastInteraction?.takeUnless { it.playerId == playerId },
            )
        }
    }

    private fun isExpired(target: ReplyTarget): Boolean =
        timeout().let {
            it != null && target.updatedAt.elapsedNow() > it
        }

}
