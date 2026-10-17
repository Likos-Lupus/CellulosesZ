package top.likoslupus.cellulosesz.economy.money

import java.math.BigDecimal
import java.math.RoundingMode

/**
 * An exact monetary value carried in-domain at a fixed scale of two. It is never built from a
 * floating-point value; [of] rejects a scale beyond two with [RoundingMode.UNNECESSARY] instead of
 * silently rounding, and conversion to the stored minor unit is exact.
 */
@JvmInline
internal value class Money private constructor(val decimal: BigDecimal) {

    fun toMinorUnitsExact(): Long =
        decimal.movePointRight(2).longValueExact()

    fun negate(): Money =
        Money(decimal.negate())

    fun plus(other: Money): Money =
        Money(decimal.add(other.decimal))

    fun minus(other: Money): Money =
        Money(decimal.subtract(other.decimal))

    fun compareTo(other: Money): Int =
        decimal.compareTo(other.decimal)

    val isPositive: Boolean
        get() = decimal.signum() > 0

    val isZero: Boolean
        get() = decimal.signum() == 0

    val isNonNegative: Boolean
        get() = decimal.signum() >= 0

    override fun toString(): String = decimal.toPlainString()

    companion object {

        fun of(value: BigDecimal): Money =
            Money(
                value.setScale(
                    2,
                    RoundingMode.UNNECESSARY
                )
            )

        fun parse(input: String): Money =
            of(BigDecimal(input))

        fun fromMinorUnits(minor: Long): Money =
            Money(
                BigDecimal.valueOf(
                    minor,
                    2
                )
            )

        val ZERO: Money = Money(
            BigDecimal.ZERO.setScale(
                2,
                RoundingMode.UNNECESSARY
            )
        )

    }

}
