package top.likoslupus.cellulosesz.economy.worth

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Test
import top.likoslupus.cellulosesz.economy.money.Money
import java.time.Instant

/** The in-memory catalog and the total, Minecraft-free seed parser. */
class WorthCacheTest {

    private fun money(value: String): Money =
        Money.parse(value)

    @Test
    fun `cache stores and looks up the exact price`() {
        val cache = WorthCache()
        cache.replaceAll(
            listOf(
                WorthEntry("minecraft:dirt", money("0.10")),
                WorthEntry("minecraft:diamond", money("50.00")),
            )
        )

        assertEquals(2, cache.size())
        assertEquals(
            money("50.00"),
            cache.unitPrice("minecraft:diamond")
        )
        assertNull(cache.unitPrice("minecraft:emerald"))

        cache.put("minecraft:emerald", money("40.00"))
        assertEquals(
            money("40.00"),
            cache.unitPrice("minecraft:emerald")
        )
        assertEquals(
            listOf("minecraft:diamond", "minecraft:dirt", "minecraft:emerald"),
            cache.snapshot().map { it.itemId },
        )
    }

    @Test
    fun `seed parser reads tab-separated entries and skips comments and blanks`() {
        val text = """
            # a comment
            minecraft:dirt	0.10
            minecraft:diamond	50.00
            """.trimIndent()

        val records = WorthSeedParser.parse(text, Instant.EPOCH)

        assertEquals(
            listOf("minecraft:dirt", "minecraft:diamond"),
            records.map { it.itemId },
        )
        assertEquals(
            10L,
            records.first().priceMinor
        )
        assertEquals(
            WorthSeedParser.SEED_AUTHOR,
            records.first().updatedBy
        )
    }

    @Test
    fun `seed parser skips malformed, unpriced, unknown-namespace and non-positive lines`() {
        val text = listOf(
            "not-an-id\t5.00",
            "minecraft:dirt\tnot-a-price",
            "minecraft:dirt\t0.00",
            "minecraft:dirt\t-1.00",
            "minecraft:dirt\t1.234",
            "minecraftoak_log\t5.00",
            "minecraft:stone\t1.00",
        ).joinToString("\n")

        val records = WorthSeedParser.parse(text, Instant.EPOCH)

        assertEquals(
            listOf("minecraft:stone"),
            records.map { it.itemId }
        )
    }

}
