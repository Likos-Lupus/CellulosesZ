package top.likoslupus.cellulosesz.communication.mail

import java.util.*
import kotlin.time.Duration.Companion.minutes
import kotlin.time.TimeMark
import kotlin.time.TimeSource

/**
 * Per-sender fixed-window rate limiter for mail. Transient (reset on restart) and monotonic;
 * one spammer can never exhaust a global server quota.
 */
internal class MailRateLimiter(
    private val timeSource: TimeSource = TimeSource.Monotonic,
) {

    private class Window(
        var start: TimeMark,
        var count: Int,
    )

    private val windows = HashMap<UUID, Window>()

    fun tryAcquire(
        senderId: UUID,
        limitPerMinute: Int,
    ): Boolean {
        if (limitPerMinute <= 0) {
            return false
        }

        val now = timeSource.markNow()
        val window = windows.getOrPut(senderId) { Window(now, 0) }
        if (window.start.elapsedNow() >= 1.minutes) {
            window.start = now
            window.count = 0
        }
        if (window.count >= limitPerMinute) {
            return false
        }

        window.count++
        return true
    }

}
