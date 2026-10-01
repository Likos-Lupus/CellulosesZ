package top.likoslupus.cellulosesz.movement.teleport

import top.likoslupus.cellulosesz.movement.config.TeleportSafetySettings
import kotlin.time.Duration

/** How aggressively a destination must be made safe before committing. */
internal enum class SafetyMode {

    REQUIRE_SAFE,
    FIND_NEAREST_SAFE,
    ALLOW_UNSAFE,

}

/**
 * Immutable, self-contained teleport policy captured when an intent is built. It carries every rule
 * the coordinator needs, so policy decisions never leak back into command handlers.
 */
internal data class TeleportPolicy(
    val safetyMode: SafetyMode,
    val safety: TeleportSafetySettings,
    val delay: Duration,
    val cooldown: Duration,
    val cancelOnMove: Boolean,
    val cancelOnDamage: Boolean,
    val movementTolerance: Double,
    val dismountPassengers: Boolean,
    val recordHistory: Boolean,
)
