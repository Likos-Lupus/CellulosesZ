package top.likoslupus.cellulosesz.economy.sale

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import top.likoslupus.cellulosesz.economy.config.SellMultiplier
import java.math.BigDecimal

/** The permission-gated multiplier resolution: highest held factor wins, no match pays face value. */
class SellMultipliersTest {

    private val granted = listOf(
        SellMultiplier(
            "cellulosesz.sell.multiplier.2",
            BigDecimal("2.00")
        ),
        SellMultiplier(
            "cellulosesz.sell.multiplier.3",
            BigDecimal("3.00")
        ),
    )

    @Test
    fun `no held multiplier pays face value`() {
        assertEquals(
            BigDecimal.ONE,
            SellMultipliers.resolve(granted) { false },
        )
    }

    @Test
    fun `the highest held multiplier wins`() {
        assertEquals(
            BigDecimal("3.00"),
            SellMultipliers.resolve(granted) { true },
        )
        assertEquals(
            BigDecimal("2.00"),
            SellMultipliers.resolve(granted) {
                it.endsWith(".2")
            },
        )
    }

    @Test
    fun `blank permissions are ignored`() {
        assertEquals(
            BigDecimal.ONE,
            SellMultipliers.resolve(
                listOf(
                    SellMultiplier(
                        "",
                        BigDecimal("9.00")
                    )
                )
            ) { true },
        )
    }

}
