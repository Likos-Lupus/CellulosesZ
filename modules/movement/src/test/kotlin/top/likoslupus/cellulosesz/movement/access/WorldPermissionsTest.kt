package top.likoslupus.cellulosesz.movement.access

import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test

class WorldPermissionsTest {

    @Test
    fun `a warp node lowercases and maps disallowed characters`() {
        assertEquals(
            "cellulosesz.warp.market_plaza",
            WorldPermissions.warpNode("Market Plaza"),
        )
    }

    @Test
    fun `dots and dashes survive warp normalisation`() {
        assertEquals(
            "cellulosesz.warp.my.warp-2",
            WorldPermissions.warpNode("My.Warp-2"),
        )
    }

    @Test
    fun `normalisation is idempotent`() {
        val node = WorldPermissions.normalizeWarpName("Some Warp!")
        assertEquals(node, WorldPermissions.normalizeWarpName(node))
    }

    @Test
    fun `world nodes keep the dimension resource id`() {
        assertEquals(
            "cellulosesz.world.minecraft.the_nether",
            WorldPermissions.teleportNode("minecraft:the_nether"),
        )
        assertEquals(
            "cellulosesz.world.home.minecraft.the_end",
            WorldPermissions.homeNode("minecraft:the_end"),
        )
    }

    @Test
    fun `alias keys reject uppercase and spaces`() {
        assertTrue(WorldPermissions.isAliasKey("the_nether-2.x"))
        assertFalse(WorldPermissions.isAliasKey("The Nether"))
        assertFalse(WorldPermissions.isAliasKey(""))
    }

}
