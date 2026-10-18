package top.likoslupus.cellulosesz.administration.playerstate

import java.time.Duration
import java.time.Instant

/**
 * Pure `/heal` cooldown arithmetic. A cooldown of zero or less disables the limit, and a bypass
 * (the `HEAL_COOLDOWN_BYPASS` permission) short-circuits it entirely.
 */
internal object HealCooldown {

    fun remaining(
        lastUsed: Instant?,
        now: Instant,
        cooldownSeconds: Long,
        bypass: Boolean,
    ): Duration? {
        if (bypass || cooldownSeconds <= 0L || lastUsed == null) {
            return null
        }
        val limit = Duration.ofSeconds(cooldownSeconds)
        val elapsed = Duration.between(lastUsed, now)
        if (elapsed >= limit) {
            return null
        }
        return limit.minus(elapsed)
    }

}
