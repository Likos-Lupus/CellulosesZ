package top.likoslupus.cellulosesz.utility.unlimited

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

class UnlimitedTopUpTest {

    @Test
    fun `a single unlimited item is topped up to two`() {
        val plan = UnlimitedTopUp.plan(
            listOf(UnlimitedSlot(1, "minecraft:stone", 1)),
            setOf("minecraft:stone"),
        )

        assertEquals(listOf(UnlimitedSlot(1, "minecraft:stone", 2)), plan)
    }

    @Test
    fun `larger stacks are never reduced`() {
        val plan = UnlimitedTopUp.plan(
            listOf(UnlimitedSlot(1, "minecraft:stone", 64)),
            setOf("minecraft:stone"),
        )

        assertTrue(plan.isEmpty())
    }

    @Test
    fun `items not marked unlimited are ignored`() {
        val plan = UnlimitedTopUp.plan(
            listOf(UnlimitedSlot(1, "minecraft:stone", 1)),
            setOf("minecraft:dirt"),
        )

        assertTrue(plan.isEmpty())
    }

    @Test
    fun `empty slots are ignored`() {
        val plan = UnlimitedTopUp.plan(
            listOf(UnlimitedSlot(1, "minecraft:stone", 0)),
            setOf("minecraft:stone"),
        )

        assertTrue(plan.isEmpty())
    }

}
