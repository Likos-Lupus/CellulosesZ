package top.likoslupus.cellulosesz.api.kit

import top.likoslupus.cellulosesz.api.validation.requireNonBlank
import top.likoslupus.cellulosesz.api.validation.requireNonNegative

/** Lossless inventory stack and its original player-inventory slot. */
@JvmRecord
public data class KitItem(
    public val slot: Int,
    public val stack: String
) {

    init {
        slot.requireNonNegative { "slot" }
        stack.requireNonBlank { "stack" }
    }

}
