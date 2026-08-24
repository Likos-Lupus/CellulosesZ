package top.likoslupus.cellulosesz.api.item

import top.likoslupus.cellulosesz.api.validation.requireInRange
import top.likoslupus.cellulosesz.api.validation.requirePositive

@JvmRecord
public data class ItemGrantResult(
    public val requested: Int,
    public val granted: Int
) {

    init {
        requested.requirePositive { "requested" }
        granted.requireInRange(0, requested) { "granted" }
    }

    public fun complete(): Boolean = requested == granted

}
