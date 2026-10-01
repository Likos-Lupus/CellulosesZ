package top.likoslupus.cellulosesz.movement.config

import kotlinx.serialization.Serializable

/** Root of the movement bounded context's configuration. */
@Serializable
public data class MovementSettings(
    public val teleport: TeleportSettings = TeleportSettings(),
    public val requests: TeleportRequestSettings = TeleportRequestSettings(),
    public val homes: HomeSettings = HomeSettings(),
    public val history: HistorySettings = HistorySettings(),
)
