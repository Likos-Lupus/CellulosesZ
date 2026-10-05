package top.likoslupus.cellulosesz.utility.config

import kotlinx.serialization.Serializable

/** Product policy for item/inventory convenience features. */
@Serializable
public data class ItemUtilitySettings(
    public val repairEnabled: Boolean = true,
    public val repairEnchanted: Boolean = false,
    public val repairAllIncludesArmor: Boolean = true,
    public val moreEnabled: Boolean = false,
    public val condenseEnabled: Boolean = false,
)
