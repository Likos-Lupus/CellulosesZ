package top.likoslupus.cellulosesz.player.afk

import top.likoslupus.cellulosesz.core.player.SleepExclusionRegistry
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
 * clears auto AFK (and manual AFK when [AfkSettings.manualClearOnActivity] is set). Interactions
 * reported through [cancelForActivity] clear AFK when their own `cancelOn*` setting is enabled.
 *
 * The current AFK set is mirrored into [AfkState] for the movement/fishing mixins, and AFK players
 * are added to [SleepExclusionRegistry] while [AfkSettings.sleepIgnoresAfk] is set.
 */
internal class AfkService(
    private val settings: () -> AfkSettings,
    private val clock: Clock,
) {

    private val manual = HashMap<UUID, String?>()
    private val auto = HashSet<UUID>()
    private val activity = HashMap<UUID, Activity>()
    private val afkSince = HashMap<UUID, Instant>()

    fun enabled(): Boolean =
        settings().enabled

    fun isAfk(playerId: UUID): Boolean =
        manual.containsKey(playerId) || auto.contains(playerId)

    fun reason(playerId: UUID): String? =
        manual[playerId]

    /** Seconds the player has been AFK, or null when they are active. */
    fun afkSeconds(playerId: UUID, now: Instant): Long? {
        if (!isAfk(playerId)) {
            return null
        }
        val since = afkSince[playerId] ?: return 0L
        return ((now.toEpochMilli() - since.toEpochMilli()) / 1000L).coerceAtLeast(0L)
    }

    /**
     * Whether an AFK player should be kicked now. The caller resolves the `AFK_BYPASS` capability and
     * passes it as [exempt]; a disabled feature or a zero threshold never kicks.
     */
    fun shouldKick(
        playerId: UUID,
        now: Instant,
        exempt: Boolean,
    ): Boolean {
        val threshold = settings().kickAfterSeconds
        if (!settings().enabled || threshold <= 0 || exempt) {
            return false
        }
        val seconds = afkSeconds(playerId, now) ?: return false
        return seconds >= threshold
    }

    /** Toggles the caller's AFK state; entering sets [reason]. */
    fun toggle(
        playerId: UUID,
        playerName: String,
        reason: String?,
    ): AfkChange =
        when {
            isAfk(playerId) -> {
                clearState(playerId)
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
                markAfk(playerId)
                AfkChange(
                    playerId,
                    playerName,
                    afk = true,
                    reason = reason
                )
            }
        }

    /**
     * Clears AFK after a reported interaction. Only triggers whose `cancelOn*` setting is enabled can
     * clear the state, so enabling one never widens another.
     */
    fun cancelForActivity(
        playerId: UUID,
        playerName: String,
        triggers: Set<AfkCancelTrigger>,
    ): AfkChange? {
        if (triggers.isEmpty() || !isAfk(playerId)) {
            return null
        }
        val config = settings()
        val permitted = triggers.any { trigger ->
            when (trigger) {
                AfkCancelTrigger.INTERACT -> config.cancelOnInteract
                AfkCancelTrigger.CHAT -> config.cancelOnChat
                AfkCancelTrigger.FISH -> config.cancelOnFish
            }
        }
        if (!permitted) {
            return null
        }
        clearState(playerId)
        return AfkChange(playerId, playerName, afk = false, reason = null)
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
                clearState(playerId)
                return AfkChange(playerId, playerName, afk = false, reason = null)
            }
            if (auto.remove(playerId)) {
                leaveAfk(playerId)
                return AfkChange(playerId, playerName, afk = false, reason = null)
            }
            return null
        }

        val idleMillis = now.toEpochMilli() - current.lastActiveAt.toEpochMilli()
        if (!isAfk(playerId)
            && idleMillis >= settings().autoAfterSeconds * 1000L
        ) {
            auto.add(playerId)
            markAfk(playerId)
            return AfkChange(playerId, playerName, afk = true, reason = null)
        }
        return null
    }

    fun clear(playerId: UUID) {
        manual.remove(playerId)
        auto.remove(playerId)
        activity.remove(playerId)
        afkSince.remove(playerId)
        includeSleep(playerId)
        AfkState.publish(manual.keys + auto)
    }

    fun clearAll() {
        manual.keys.toList().forEach(::includeSleep)
        auto.forEach(::includeSleep)
        manual.clear()
        auto.clear()
        activity.clear()
        afkSince.clear()
        AfkState.publish(emptySet())
    }

    /** Re-applies the sleep-exclusion and freeze policy after a config reload. Server thread only. */
    fun reapplyPolicy() {
        AfkState.setFreezeWhileAfk(settings().freezeWhileAfk)
        (manual.keys + auto).forEach { playerId ->
            applySleep(playerId, excluded = settings().sleepIgnoresAfk)
        }
    }

    private fun clearState(playerId: UUID) {
        manual.remove(playerId)
        auto.remove(playerId)
        leaveAfk(playerId)
    }

    private fun markAfk(playerId: UUID) {
        if (!afkSince.containsKey(playerId)) {
            afkSince[playerId] = clock.instant()
        }
        AfkState.publish(manual.keys + auto)
        applySleep(playerId, settings().sleepIgnoresAfk)
    }

    private fun leaveAfk(playerId: UUID) {
        afkSince.remove(playerId)
        activity[playerId]?.lastActiveAt = clock.instant()
        AfkState.publish(manual.keys + auto)
        includeSleep(playerId)
    }

    private fun applySleep(playerId: UUID, excluded: Boolean) {
        if (excluded) {
            SleepExclusionRegistry.exclude(playerId)
        } else {
            SleepExclusionRegistry.include(playerId)
        }
    }

    private fun includeSleep(playerId: UUID) {
        SleepExclusionRegistry.include(playerId)
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
