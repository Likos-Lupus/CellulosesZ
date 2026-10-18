package top.likoslupus.cellulosesz.core.player

import java.util.*

/**
 * Process-wide set of online players who must not count towards the sleep vote: AFK players
 * (optionally) and vanished players (optionally). Both features live in different modules, so neither
 * may depend on the other; they publish their state here and the server level mixin reads the union.
 * It is an object for the same reason as [TemporaryProtectionRegistry]: a mixin cannot be handed a
 * service instance.
 */
public object SleepExclusionRegistry {

    private val excluded = HashSet<UUID>()

    public fun exclude(playerId: UUID) {
        excluded.add(playerId)
    }

    public fun include(playerId: UUID) {
        excluded.remove(playerId)
    }

    public fun isExcluded(playerId: UUID): Boolean =
        playerId in excluded

    public fun clear() {
        excluded.clear()
    }

}
