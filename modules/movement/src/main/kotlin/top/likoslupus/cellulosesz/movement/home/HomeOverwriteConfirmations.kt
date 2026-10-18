package top.likoslupus.cellulosesz.movement.home

import java.time.Instant
import java.util.*
import kotlin.time.Duration
import kotlin.time.Duration.Companion.seconds

/**
 * Short-lived, in-memory `/sethome` overwrite confirmations. A first `/sethome <name>` that would
 * replace an existing home records the request; running the identical command again inside
 * [window] confirms it. State never crosses a restart and is cleared on quit.
 */
internal class HomeOverwriteConfirmations(
    private val window: Duration = 30.seconds,
    private val now: () -> Instant = Instant::now,
) {

    private val pending = HashMap<UUID, Pending>()

    /** Records that [playerId] was asked to confirm overwriting [name]. */
    fun remember(playerId: UUID, name: String) {
        pending[playerId] = Pending(name, now())
    }

    /**
     * True when [playerId] already confirmed overwriting [name] within [window]; the entry is
     * consumed. A stale or mismatched entry is discarded and returns false.
     */
    fun confirm(playerId: UUID, name: String): Boolean {
        val entry = pending.remove(playerId)
            ?: return false
        val fresh = entry.recordedAt.plusMillis(window.inWholeMilliseconds).isAfter(now())
        return entry.name == name && fresh
    }

    fun clear(playerId: UUID) {
        pending.remove(playerId)
    }

    private data class Pending(
        val name: String,
        val recordedAt: Instant,
    )

}
