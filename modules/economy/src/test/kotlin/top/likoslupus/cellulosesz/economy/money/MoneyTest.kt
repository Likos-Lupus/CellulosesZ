package top.likoslupus.cellulosesz.economy.money

import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test
import java.math.BigDecimal

class MoneyTest {

    @Test
    fun `normalises to two fractional digits`() {
        assertEquals(
            BigDecimal("1.00"),
            Money.of(BigDecimal("1")).decimal
        )
        assertEquals(
            BigDecimal("1.20"),
            Money.of(BigDecimal("1.2")).decimal
        )
        assertEquals(
            BigDecimal("1.20"),
            Money.of(BigDecimal("1.20")).decimal
        )
    }

    @Test
    fun `rejects a scale beyond two instead of rounding`() {
        assertThrows(ArithmeticException::class.java) {
            Money.of(BigDecimal("1.234"))
        }
    }

    @Test
    fun `minor unit conversion is exact both ways`() {
        assertEquals(
            12345L,
            Money.parse("123.45").toMinorUnitsExact()
        )
        assertEquals(
            Money.parse("123.45"),
            Money.fromMinorUnits(12345L)
        )
        assertEquals(
            Money.parse("0.00"),
            Money.fromMinorUnits(0L)
        )
    }

    @Test
    fun `parsing rejects malformed input`() {
        assertNull(MoneyParsing.parse("1e3"))
        assertNull(MoneyParsing.parse("-1"))
        assertNull(MoneyParsing.parse(""))
        assertNull(MoneyParsing.parse("1,000.00"))
        assertNull(MoneyParsing.parse("NaN"))
        assertNull(MoneyParsing.parse("1.234"))
        assertEquals(
            Money.parse("0.00"),
            MoneyParsing.parse("0")
        )
        assertEquals(
            Money.parse("12.50"),
            MoneyParsing.parse("12.5")
        )
    }

    @Test
    fun `sign predicates are exact`() {
        assertEquals(
            true,
            Money.parse("0.01").isPositive
        )
        assertEquals(
            false,
            Money.parse("0.00").isPositive
        )
        assertEquals(
            true,
            Money.ZERO.isZero
        )
        assertEquals(
            false,
            Money.parse("-0.01").negate().isNegativeSelf()
        )
    }

    private fun Money.isNegativeSelf(): Boolean =
        decimal.signum() < 0

}
