package top.likoslupus.cellulosesz.application.config

import kotlinx.serialization.Serializable
import top.likoslupus.cellulosesz.communication.MessagingSettings
import top.likoslupus.cellulosesz.movement.config.HomeSettings
import top.likoslupus.cellulosesz.movement.config.TeleportRequestSettings

public const val CURRENT_SCHEMA_VERSION: Int = 1

/**
 * Root configuration. Feature owners define their own settings types; this schema only aggregates
 * them under the same JSON keys users already know.
 */
@Serializable
public data class CellulosesConfig(
    public val schemaVersion: Int = CURRENT_SCHEMA_VERSION,
    public val diagnostics: DiagnosticsConfig = DiagnosticsConfig(),
    public val homes: HomeSettings = HomeSettings(),
    public val teleportRequests: TeleportRequestSettings = TeleportRequestSettings(),
    public val messaging: MessagingSettings = MessagingSettings(),
)

@Serializable
public data class DiagnosticsConfig(
    public val verboseLogging: Boolean = false,
)
