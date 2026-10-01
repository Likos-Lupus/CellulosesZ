package top.likoslupus.cellulosesz.config

import kotlinx.serialization.Serializable

internal const val CURRENT_SCHEMA_VERSION: Int = 1

@Serializable
internal data class CellulosesConfig(
    val schemaVersion: Int = CURRENT_SCHEMA_VERSION,
    val diagnostics: DiagnosticsConfig = DiagnosticsConfig(),
    val homes: HomeConfig = HomeConfig(),
    val teleportRequests: TeleportRequestConfig = TeleportRequestConfig(),
    val messaging: MessagingConfig = MessagingConfig(),
)

@Serializable
internal data class DiagnosticsConfig(
    val verboseLogging: Boolean = false,
)

@Serializable
internal data class HomeConfig(
    val maxPerPlayer: Int = 10,
    val defaultName: String = "home",
)

@Serializable
internal data class TeleportRequestConfig(
    val timeoutSeconds: Long = 60,
)

@Serializable
internal data class MessagingConfig(
    val enabled: Boolean = true,
)
