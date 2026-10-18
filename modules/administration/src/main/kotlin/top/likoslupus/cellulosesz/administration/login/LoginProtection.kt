package top.likoslupus.cellulosesz.administration.login

import java.time.Instant
import java.util.*

/**
 * Server-thread confined memory of when each online player joined. It backs login PvP protection:
 * a player who joined less than the configured delay ago may neither deal nor take damage. It is a
 * plain value holder with no Minecraft dependency so the policy can be unit-tested.
 */
internal class LoginProtection {

    private val joinedAt = HashMap<UUID, Instant>()

    fun recordJoin(playerId: UUID, now: Instant) {
        joinedAt[playerId] = now
    }

    fun forget(playerId: UUID) {
        joinedAt.remove(playerId)
    }

    fun clear() {
        joinedAt.clear()
    }

    fun joinInstant(playerId: UUID): Instant? =
        joinedAt[playerId]

    /** True while [playerId] is inside the protection window that started at their join. */
    fun isProtected(
        playerId: UUID,
        now: Instant,
        delaySeconds: Int
    ): Boolean {
        if (delaySeconds <= 0) {
            return false
        }
        val joined = joinedAt[playerId]
            ?: return false
        return now.isBefore(
            joined.plusSeconds(delaySeconds.toLong())
        )
    }

}
