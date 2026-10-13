package top.likoslupus.cellulosesz.communication.config

import kotlinx.serialization.Serializable

/**
 * Server info pages (`/motd`, `/rules`, `/info`, `/customtext`). Pages are Markdown files under
 * `<config-dir>/cellulosesz/<rootDirectory>/<lang>/<page>.md`, read on demand so an edit is picked
 * up without a restart.
 */
@Serializable
public data class ServerPageSettings(
    public val enabled: Boolean = true,
    public val rootDirectory: String = "pages",
    public val maxPageBytes: Int = 65536,
    public val maxPages: Int = 128,
    public val showMotdOnJoin: Boolean = false,
)
