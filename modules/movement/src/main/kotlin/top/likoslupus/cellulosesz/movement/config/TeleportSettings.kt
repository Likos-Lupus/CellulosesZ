package top.likoslupus.cellulosesz.movement.config

import kotlinx.serialization.Serializable

/** Policy defaults applied to every teleport intent; per-cause overrides are derived from these. */
@Serializable
public data class TeleportSettings(
    public val delaySeconds: Long = 0,
    public val cooldownSeconds: Long = 0,
    public val cancelOnMove: Boolean = true,
    public val cancelOnDamage: Boolean = true,
    public val movementTolerance: Double = 0.15,
    public val dismountPassengers: Boolean = true,
    public val safety: TeleportSafetySettings = TeleportSafetySettings(),
)
