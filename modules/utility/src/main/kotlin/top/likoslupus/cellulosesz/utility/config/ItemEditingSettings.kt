package top.likoslupus.cellulosesz.utility.config

import kotlinx.serialization.Serializable

/** Product policy for holding-item editing (`/hat`, `/itemname`, `/itemlore`, `/clearinventory`). */
@Serializable
public data class ItemEditingSettings(
    public val maxItemNameLength: Int = 64,
    public val maxLoreLines: Int = 10,
    public val maxLoreLineLength: Int = 80,
    public val allowAnyItemAsHat: Boolean = false,
    public val clearConfirmationSeconds: Int = 15,
)
