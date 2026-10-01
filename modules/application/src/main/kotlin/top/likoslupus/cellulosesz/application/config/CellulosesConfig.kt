package top.likoslupus.cellulosesz.application.config

import kotlinx.serialization.Serializable
import top.likoslupus.cellulosesz.communication.MessagingSettings
import top.likoslupus.cellulosesz.movement.config.MovementSettings

public const val CURRENT_SCHEMA_VERSION: Int = 1

/**
 * Root configuration. Feature owners define their own settings types; this schema only aggregates
 * them under stable JSON keys.
 */
@Serializable
public data class CellulosesConfig(
    public val schemaVersion: Int = CURRENT_SCHEMA_VERSION,
    public val diagnostics: DiagnosticsConfig = DiagnosticsConfig(),
    public val movement: MovementSettings = MovementSettings(),
    public val messaging: MessagingSettings = MessagingSettings(),
)

@Serializable
public data class DiagnosticsConfig(
    public val verboseLogging: Boolean = false,
)
