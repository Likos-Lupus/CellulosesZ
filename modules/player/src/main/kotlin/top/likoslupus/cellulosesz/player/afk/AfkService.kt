package top.likoslupus.cellulosesz.player.afk

import top.likoslupus.cellulosesz.player.config.AfkSettings
import java.time.Clock
import java.time.Instant
import java.util.*
import kotlin.math.abs
import kotlin.math.max
import kotlin.math.sqrt

/** A change in a player's AFK state, with the name needed for announcements. */
internal data class AfkChange(
    val playerId: UUID,
    val playerName: String,
    val afk: Boolean,
    val reason: String?,
)

/**
 * Session-only AFK state. Every method is server-thread confined; nothing is persisted, so a restart
 * starts everyone active. Movement and rotation above their thresholds count as activity, which
 * clears auto AFK (and manual AFK when [AfkSettings.manualClearOnActivity] is set).
 */
internal class AfkService(
    private val settings: () -> AfkSettings,
    private val clock: Clock,
) {

    private val manual = HashMap<UUID, String?>()
    private val auto = HashSet<UUID>()
    private val activity = HashMap<UUID, Activity>()

    fun enabled(): Boolean =
        settings().enabled

    fun isAfk(playerId: UUID): Boolean =
        manual.containsKey(playerId) || auto.contains(playerId)

    fun reason(playerId: UUID): String? =
        manual[playerId]

    /** Toggles the caller's AFK state; entering sets [reason]. */
    fun toggle(
        playerId: UUID,
        playerName: String,
        reason: String?
    ): AfkChange =
        when {
            isAfk(playerId) -> {
                manual.remove(playerId)
                auto.remove(playerId)
                AfkChange(
                    playerId,
                    playerName,
                    afk = false,
                    reason = null
                )
            }

            else -> {
                manual[playerId] = reason
                auto.remove(playerId)
                AfkChange(
                    playerId,
                    playerName,
                    afk = true,
                    reason = reason
                )
            }
        }

    /** Server-thread tick. Returns a change only when the AFK state actually flips. */
    fun onTick(
        playerId: UUID,
        playerName: String,
        x: Double,
        y: Double,
        z: Double,
        yaw: Float,
        pitch: Float,
    ): AfkChange? {
        if (!settings().enabled) {
            return null
        }
        val now = clock.instant()
        val current = activity.getOrPut(playerId) {
            Activity(
                lastX = x,
                lastY = y,
                lastZ = z,
                lastYaw = yaw,
                lastPitch = pitch,
                lastActiveAt = now,
                ticks = 0
            )
        }
        current.ticks++
        if (current.ticks < settings().checkIntervalTicks.coerceAtLeast(1)) {
            return null
        }
        current.ticks = 0

        val dx = x - current.lastX
        val dy = y - current.lastY
        val dz = z - current.lastZ
        val movement = sqrt(dx * dx + dy * dy + dz * dz)
        val rotation = max(
            abs(yaw - current.lastYaw),
            abs(pitch - current.lastPitch)
        )
        current.lastX = x
        current.lastY = y
        current.lastZ = z
        current.lastYaw = yaw
        current.lastPitch = pitch

        val active = movement > settings().movementThreshold
                || rotation > settings().rotationThresholdDegrees
        if (active) {
            current.lastActiveAt = now
            if (settings().manualClearOnActivity && manual.containsKey(playerId)) {
                manual.remove(playerId)
                return AfkChange(playerId, playerName, afk = false, reason = null)
            }
            if (auto.remove(playerId)) {
                return AfkChange(playerId, playerName, afk = false, reason = null)
            }
            return null
        }

        val idleMillis = now.toEpochMilli() - current.lastActiveAt.toEpochMilli()
        if (!isAfk(playerId)
            && idleMillis >= settings().autoAfterSeconds * 1000L
        ) {
            auto.add(playerId)
            return AfkChange(playerId, playerName, afk = true, reason = null)
        }
        return null
    }

    fun clear(playerId: UUID) {
        manual.remove(playerId)
        auto.remove(playerId)
        activity.remove(playerId)
    }

    fun clearAll() {
        manual.clear()
        auto.clear()
        activity.clear()
    }

    private class Activity(
        var lastX: Double,
        var lastY: Double,
        var lastZ: Double,
        var lastYaw: Float,
        var lastPitch: Float,
        var lastActiveAt: Instant,
        var ticks: Int,
    )

}
