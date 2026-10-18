package top.likoslupus.cellulosesz.economy.config

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/**
 * Item-worth policy. The worth catalog is the server's buy-back price list; when the table is empty
 * it may be primed once from the resource bundled with the distribution.
 */
@Serializable
public data class WorthSettings(
    /**
     * Import `cellulosesz/worth/default.tsv` exactly once, and only while the worth table is empty.
     * Disable it to start with a blank catalog that administrators fill with `/setworth`.
     */
    @SerialName("seedBundledDefaults")
    public val seedBundledDefaults: Boolean = true,
)
