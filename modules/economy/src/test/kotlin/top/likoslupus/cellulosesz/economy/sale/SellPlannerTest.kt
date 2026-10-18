package top.likoslupus.cellulosesz.economy.sale

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertInstanceOf
import org.junit.jupiter.api.Test
import java.math.BigDecimal

/**
 * The pure sale math: exact stack set, exact total, multiplier application, and the skip/refuse
 * policies. No Minecraft, no storage.
 */
class SellPlannerTest {

    private val one = BigDecimal("1.00")

    private fun candidate(
        slot: Int,
        item: String = "minecraft:dirt",
        count: Int = 1,
        price: Long? = 100L,
        named: Boolean = false,
    ) =
        SellCandidate(
            slot,
            item,
            count,
            price,
            named
        )

    @Test
    fun `sums exact totals with the multiplier applied once to the total`() {
        val result = SellPlanner.plan(
            candidates = listOf(candidate(0, count = 3, price = 150L)),
            namedPolicy = SkipPolicy.REFUSE,
            unpricedPolicy = SkipPolicy.REFUSE,
            multiplier = BigDecimal("2.00"),
            maximumMinor = Long.MAX_VALUE,
        )

        val planned = assertInstanceOf(
            SellPlanResult.Planned::class.java,
            result
        )
        assertEquals(3, planned.itemCount)
        assertEquals(900L, planned.totalMinor)
        assertEquals(
            listOf(
                SellLine(
                    0,
                    "minecraft:dirt",
                    3,
                    150L
                )
            ),
            planned.lines
        )
    }

    @Test
    fun `truncates a fractional multiplier toward zero instead of rounding up`() {
        val result = SellPlanner.plan(
            candidates = listOf(candidate(0, count = 1, price = 101L)),
            namedPolicy = SkipPolicy.REFUSE,
            unpricedPolicy = SkipPolicy.REFUSE,
            multiplier = BigDecimal("1.50"),
            maximumMinor = Long.MAX_VALUE,
        )

        val planned = assertInstanceOf(
            SellPlanResult.Planned::class.java,
            result
        )
        assertEquals(151L, planned.totalMinor)
    }

    @Test
    fun `splits a target across slots and respects the limit`() {
        val result = SellPlanner.plan(
            candidates = listOf(
                candidate(0, count = 40, price = 10L),
                candidate(1, count = 40, price = 10L),
            ),
            namedPolicy = SkipPolicy.REFUSE,
            unpricedPolicy = SkipPolicy.REFUSE,
            multiplier = one,
            maximumMinor = Long.MAX_VALUE,
            limit = 50,
        )

        val planned = assertInstanceOf(
            SellPlanResult.Planned::class.java,
            result
        )
        assertEquals(50, planned.itemCount)
        assertEquals(listOf(40, 10), planned.lines.map { it.count })
        assertEquals(500L, planned.totalMinor)
    }

    @Test
    fun `skips named candidates under SKIP and refuses them under REFUSE`() {
        val skipped = SellPlanner.plan(
            candidates = listOf(
                candidate(0, named = true),
                candidate(1, count = 2)
            ),
            namedPolicy = SkipPolicy.SKIP,
            unpricedPolicy = SkipPolicy.REFUSE,
            multiplier = one,
            maximumMinor = Long.MAX_VALUE,
        )
        val planned = assertInstanceOf(
            SellPlanResult.Planned::class.java,
            skipped
        )
        assertEquals(
            listOf(1),
            planned.lines.map { it.slot }
        )

        val refused = SellPlanner.plan(
            candidates = listOf(candidate(0, named = true)),
            namedPolicy = SkipPolicy.REFUSE,
            unpricedPolicy = SkipPolicy.REFUSE,
            multiplier = one,
            maximumMinor = Long.MAX_VALUE,
        )
        assertEquals(
            SellPlanResult.NamedNotAllowed("minecraft:dirt"),
            refused,
        )
    }

    @Test
    fun `skips unpriced candidates under SKIP and refuses them under REFUSE`() {
        val candidate = candidate(0, price = null)
        assertEquals(
            SellPlanResult.Unpriced("minecraft:dirt"),
            SellPlanner.plan(
                listOf(candidate),
                SkipPolicy.REFUSE,
                SkipPolicy.REFUSE,
                one,
                Long.MAX_VALUE,
            ),
        )
        assertEquals(
            SellPlanResult.Nothing,
            SellPlanner.plan(
                listOf(candidate),
                SkipPolicy.SKIP,
                SkipPolicy.SKIP,
                one,
                Long.MAX_VALUE,
            ),
        )
    }

    @Test
    fun `rejects an empty plan and a zero total`() {
        assertEquals(
            SellPlanResult.Nothing,
            SellPlanner.plan(
                emptyList(),
                SkipPolicy.REFUSE,
                SkipPolicy.REFUSE,
                one,
                Long.MAX_VALUE,
            ),
        )
        assertEquals(
            SellPlanResult.ZeroValue,
            SellPlanner.plan(
                listOf(candidate(0)),
                SkipPolicy.REFUSE,
                SkipPolicy.REFUSE,
                BigDecimal.ZERO,
                Long.MAX_VALUE,
            ),
        )
    }

    @Test
    fun `rejects a total above the configured ceiling`() {
        assertEquals(
            SellPlanResult.ExceedsCap,
            SellPlanner.plan(
                listOf(candidate(0, count = 10, price = 100L)),
                SkipPolicy.REFUSE,
                SkipPolicy.REFUSE,
                one,
                maximumMinor = 500L,
            ),
        )
    }

}
