package top.likoslupus.cellulosesz.player.session

import java.time.Instant
import java.util.*
import kotlin.time.Duration
import kotlin.time.Duration.Companion.milliseconds

/** How a session ended. Persisted by name; [NORMAL] is a clean disconnect. */
internal enum class SessionCloseKind {

    NORMAL,
    SERVER_STOP,
    RECOVERED,

}

/**
 * One persisted play session. Immutable and safe to cross suspension; it never holds a live
 * Minecraft object.
 */
internal data class PlayerSession(
    val sessionId: UUID,
    val playerId: UUID,
    val loginAt: Instant,
    val lastHeartbeatAt: Instant,
    val closedAt: Instant?,
    val closeKind: SessionCloseKind?,
) {

    /** Elapsed play time as of [now]; an open session runs up to [now]. Never negative. */
    fun durationAt(now: Instant): Duration {
        val end = closedAt ?: now
        val millis = (end.toEpochMilli() - loginAt.toEpochMilli()).coerceAtLeast(0L)
        return millis.milliseconds
    }

}
