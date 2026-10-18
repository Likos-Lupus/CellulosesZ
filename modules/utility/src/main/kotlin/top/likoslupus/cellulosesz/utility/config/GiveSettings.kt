package top.likoslupus.cellulosesz.utility.config

import kotlinx.serialization.Serializable

/**
 * Product policy for `/give`, `/item` and the other item-granting tools. [blacklist] entries are
 * registry ids (`minecraft:bedrock`); an id without a namespace is treated as `minecraft:`.
 */
@Serializable
public data class GiveSettings(
    public val defaultAmount: Int = 1,
    public val maxAmount: Int = 2304,
    public val blacklist: List<String> = emptyList(),
    /** When true, an item that does not fit is dropped at the target's feet instead of failing. */
    public val dropIfFull: Boolean = false,
)
