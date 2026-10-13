package top.likoslupus.cellulosesz.administration.playerstate

import java.util.*

/** The base values of a player's walk and fly speed attributes. */
internal data class SpeedSnapshot(
    val walk: Double,
    val fly: Double,
)

/** Server-thread confined read/write seam for the two speed attributes. */
internal interface SpeedAttributeAccess {

    /** Null when the player is no longer online. */
    fun readSpeed(playerId: UUID): SpeedSnapshot?

    fun writeSpeed(playerId: UUID, value: SpeedSnapshot)

}

/** Pure clamping for the configured `[minimum, maximum]` speed multiplier range. */
internal object SpeedMultiplier {

    fun clamp(value: Double, minimum: Double, maximum: Double): Double {
        val low = minOf(minimum, maximum)
        val high = maxOf(minimum, maximum)
        return when {
            value.isNaN() -> low
            else -> value.coerceIn(low, high)
        }
    }

}

/**
 * Session-scoped `/speed` state. The first modification snapshots the player's original base speed
 * values; later modifications are computed from that snapshot, and [restore] writes it back. The map
 * is in-memory and cleared on quit and on server stop, so nothing survives reconnection.
 */
internal class SpeedSessions(
    private val access: SpeedAttributeAccess,
) {

    private val snapshots = HashMap<UUID, SpeedSnapshot>()

    /** Returns false when the player is offline. */
    fun apply(playerId: UUID, multiplier: Double): Boolean {
        val current = access.readSpeed(playerId)
            ?: return false
        val snapshot = snapshots.getOrPut(playerId) { current }
        access.writeSpeed(
            playerId,
            SpeedSnapshot(
                walk = snapshot.walk * multiplier,
                fly = snapshot.fly * multiplier,
            ),
        )
        return true
    }

    fun restore(playerId: UUID) {
        val snapshot = snapshots.remove(playerId)
            ?: return
        access.writeSpeed(playerId, snapshot)
    }

    fun restoreAll() {
        snapshots.keys.toList().forEach(::restore)
    }

    fun isTracked(playerId: UUID): Boolean =
        snapshots.containsKey(playerId)

}
