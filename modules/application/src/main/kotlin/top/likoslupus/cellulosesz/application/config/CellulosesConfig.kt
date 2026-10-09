package top.likoslupus.cellulosesz.application.config

import kotlinx.serialization.Serializable
import top.likoslupus.cellulosesz.administration.config.AdministrationSettings
import top.likoslupus.cellulosesz.communication.config.MessagingSettings
import top.likoslupus.cellulosesz.foundation.database.DatabaseSettings
import top.likoslupus.cellulosesz.movement.config.MovementSettings
import top.likoslupus.cellulosesz.utility.config.UtilitySettings

/**
 * Root configuration. Feature owners define their own settings types; this schema only aggregates
 * them under stable TOML tables. There is no schema version: this is a pre-release project and new
 * fields are added as defaults, while unknown keys are rejected.
 */
@Serializable
public data class CellulosesConfig(
    public val diagnostics: DiagnosticsConfig = DiagnosticsConfig(),
    public val database: DatabaseSettings = DatabaseSettings(),
    public val movement: MovementSettings = MovementSettings(),
    public val messaging: MessagingSettings = MessagingSettings(),
    public val administration: AdministrationSettings = AdministrationSettings(),
    public val utility: UtilitySettings = UtilitySettings(),
)

@Serializable
public data class DiagnosticsConfig(
    public val verboseLogging: Boolean = false,
)
