package top.likoslupus.cellulosesz.utility.config

import top.likoslupus.cellulosesz.utility.kit.KitOverflowPolicy
import kotlinx.serialization.Serializable

/** Product policy for the kit distribution feature. Machine safety caps live in the repository. */
@Serializable
public data class KitSettings(
    public val enabled: Boolean = true,
    public val maxKits: Int = 128,
    public val maxItemsPerKit: Int = 64,
    public val overflowPolicy: KitOverflowPolicy = KitOverflowPolicy.REJECT,
    public val maxCooldownSeconds: Long? = null,
    /** Move delivered armor/offhand items into empty equipment slots on claim. */
    public val autoEquip: Boolean = true,
    /** Hide kits a player has already claimed when their reuse policy is `once`. */
    public val hideClaimedOnce: Boolean = false,
)
