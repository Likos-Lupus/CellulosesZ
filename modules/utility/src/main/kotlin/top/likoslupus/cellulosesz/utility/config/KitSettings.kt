package top.likoslupus.cellulosesz.utility.config

import kotlinx.serialization.Serializable
import top.likoslupus.cellulosesz.utility.kit.KitOverflowPolicy

/** Product policy for the kit distribution feature. Machine safety caps live in the repository. */
@Serializable
public data class KitSettings(
    public val enabled: Boolean = true,
    public val maxKits: Int = 128,
    public val maxItemsPerKit: Int = 64,
    public val overflowPolicy: KitOverflowPolicy = KitOverflowPolicy.REJECT,
    public val maxCooldownSeconds: Long? = null,
)
