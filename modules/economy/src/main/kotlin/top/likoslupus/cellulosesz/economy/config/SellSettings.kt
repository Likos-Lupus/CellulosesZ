package top.likoslupus.cellulosesz.economy.config

import java.math.BigDecimal
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/**
 * Item buy-back policy. A sale credit is still one balanced ledger transaction; these flags only
 * decide *what* may be sold and *how much* the server pays.
 */
@Serializable
public data class SellSettings(
    /** When false, `/sell inventory` and `/sell blocks` additionally require `cellulosesz.sell.bulk`. */
    @SerialName("allowBulk")
    public val allowBulk: Boolean = false,
    /** When false, a stack carrying a custom name is never sold. */
    @SerialName("allowNamedItems")
    public val allowNamedItems: Boolean = false,
    /**
     * Sell-price multipliers keyed by permission. Among every listed permission the seller actually
     * holds, the one with the highest [SellMultiplier.multiplier] wins; with no match the price is
     * paid at face value. Configured ascending so the intent reads naturally.
     */
    @SerialName("multipliers")
    public val multipliers: List<SellMultiplier> = emptyList(),
)

/** One `cellulosesz.sell.multiplier.<n>` grant: the permission to test and the factor it awards. */
@Serializable
public data class SellMultiplier(
    /** A full permission node, for example `cellulosesz.sell.multiplier.2`. */
    public val permission: String = "",
    /** The factor applied to the total, kept exact (never a TOML float). */
    @Serializable(with = ExactMoneySerializer::class)
    public val multiplier: BigDecimal = BigDecimal("1.00"),
)
