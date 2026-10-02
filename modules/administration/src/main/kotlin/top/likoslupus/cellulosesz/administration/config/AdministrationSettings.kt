package top.likoslupus.cellulosesz.administration.config

import kotlinx.serialization.Serializable

/** Root of the administration bounded context's configuration. */
@Serializable
public data class AdministrationSettings(
    public val moderation: ModerationSettings = ModerationSettings(),
)
