package top.likoslupus.cellulosesz.movement.teleport.cooldown

import java.util.*
import kotlin.time.ComparableTimeMark
import kotlin.time.Duration
import kotlin.time.TimeSource

/**
 * Transient, success-only teleport cooldowns. Time comes from a [TimeSource] so tests advance a
 * `TestTimeSource` instead of mocking a wall clock.
 */
internal class TeleportCooldowns(
    private val timeSource: TimeSource.WithComparableMarks = TimeSource.Monotonic,
) {

    private val lastSuccess = HashMap<UUID, ComparableTimeMark>()

    fun remaining(playerId: UUID, cooldown: Duration): Duration? {
        if (cooldown <= Duration.ZERO) {
            return null
        }

        val mark = lastSuccess[playerId] ?: return null
        val left = cooldown - mark.elapsedNow()
        return left.takeIf { it > Duration.ZERO }
    }

    fun recordSuccess(playerId: UUID) {
        lastSuccess[playerId] = timeSource.markNow()
    }

}
