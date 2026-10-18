package top.likoslupus.cellulosesz.utility.itemtools

import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test

class ItemToolsPolicyTest {

    @Test
    fun `normalize defaults the namespace`() {
        assertEquals(
            "minecraft:dirt",
            ItemToolsPolicy.normalizeId("dirt")
        )
        assertEquals(
            "minecraft:dirt",
            ItemToolsPolicy.normalizeId("DIrt")
        )
        assertEquals(
            "mod:thing",
            ItemToolsPolicy.normalizeId("Mod:Thing")
        )
    }

    @Test
    fun `blacklist matches normalized ids`() {
        assertTrue(
            ItemToolsPolicy.isBlacklisted(
                "minecraft:bedrock",
                listOf("bedrock")
            )
        )
        assertTrue(
            ItemToolsPolicy.isBlacklisted(
                "bedrock",
                listOf("minecraft:bedrock")
            )
        )
        assertFalse(
            ItemToolsPolicy.isBlacklisted(
                "minecraft:dirt",
                listOf("bedrock")
            )
        )
    }

    @Test
    fun `resolveAmount uses the default when absent`() {
        assertEquals(
            4,
            ItemToolsPolicy.resolveAmount(null, default = 4, max = 64)
        )
    }

    @Test
    fun `resolveAmount rejects out of range values`() {
        assertNull(ItemToolsPolicy.resolveAmount(0, default = 1, max = 64))
        assertNull(ItemToolsPolicy.resolveAmount(65, default = 1, max = 64))
        assertEquals(
            64,
            ItemToolsPolicy.resolveAmount(64, default = 1, max = 64)
        )
    }

}
