package top.likoslupus.cellulosesz.movement.teleport

import top.likoslupus.cellulosesz.movement.config.TeleportSettings
import kotlin.time.Duration
import kotlin.time.Duration.Companion.seconds

/**
 * Maps a cause to a fully resolved policy. Command handlers pick a cause; they never assemble delay,
 * cooldown, safety, or cancellation rules themselves.
 */
internal fun teleportPolicyFor(cause: TeleportCause, settings: TeleportSettings): TeleportPolicy {
    val delay = settings.delaySeconds.seconds
    val cooldown = settings.cooldownSeconds.seconds
    return when (cause) {
        TeleportCause.HOME,
        TeleportCause.WARP,
        TeleportCause.SPAWN,
        TeleportCause.BACK,
        TeleportCause.REQUEST,
        TeleportCause.DIRECT -> TeleportPolicy(
            safetyMode = SafetyMode.FIND_NEAREST_SAFE,
            safety = settings.safety,
            delay = delay,
            cooldown = cooldown,
            cancelOnMove = settings.cancelOnMove,
            cancelOnDamage = settings.cancelOnDamage,
            movementTolerance = settings.movementTolerance,
            dismountPassengers = settings.dismountPassengers,
            recordHistory = true,
        )

        TeleportCause.ADMIN -> TeleportPolicy(
            safetyMode = SafetyMode.ALLOW_UNSAFE,
            safety = settings.safety,
            delay = Duration.ZERO,
            cooldown = Duration.ZERO,
            cancelOnMove = false,
            cancelOnDamage = false,
            movementTolerance = settings.movementTolerance,
            dismountPassengers = settings.dismountPassengers,
            recordHistory = true,
        )

        TeleportCause.RANDOM -> TeleportPolicy(
            safetyMode = SafetyMode.REQUIRE_SAFE,
            safety = settings.safety,
            delay = delay,
            cooldown = cooldown,
            cancelOnMove = settings.cancelOnMove,
            cancelOnDamage = settings.cancelOnDamage,
            movementTolerance = settings.movementTolerance,
            dismountPassengers = settings.dismountPassengers,
            recordHistory = true,
        )
    }
}
