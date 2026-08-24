package top.likoslupus.cellulosesz.api.economy

import java.math.BigDecimal

@JvmRecord
public data class BalanceFilter(
    public val minimum: BigDecimal?,
    public val maximum: BigDecimal?
) {

    init {
        require(!(minimum != null && maximum != null && minimum > maximum)) {
            "minimum must not exceed maximum"
        }
    }

    public companion object {

        @JvmStatic
        public fun all(): BalanceFilter = BalanceFilter(null, null)

    }

}
