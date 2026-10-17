package top.likoslupus.cellulosesz.api.economy

import java.math.BigDecimal
import java.math.RoundingMode

/**
 * An exact, non-negative monetary amount at a fixed scale of two, safe for both Kotlin and Java
 * callers. It is never built from a floating-point value.
 */
public class EconomyAmount(value: BigDecimal) {

    /** The exact value, normalised to two fractional digits. */
    public val decimal: BigDecimal = value.setScale(2, RoundingMode.UNNECESSARY)

    init {
        require(decimal.signum() >= 0) {
            "amount must not be negative"
        }
    }

    public override fun equals(other: Any?): Boolean =
        other is EconomyAmount && decimal == other.decimal

    public override fun hashCode(): Int =
        decimal.hashCode()

    public override fun toString(): String =
        decimal.toPlainString()

}
