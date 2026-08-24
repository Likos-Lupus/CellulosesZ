package top.likoslupus.cellulosesz.api.teleport

import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows

class TeleportContractsTest {

    @Test
    fun cellLocation_validatesAndFormats() {
        val location = CellLocation("minecraft:overworld", 10.5, 64.0, -20.25, 90.0f, 0.0f)
        assertEquals("minecraft:overworld", location.world)
        assertEquals(10.5, location.x)
        assertEquals(64.0, location.y)
        assertEquals(-20.25, location.z)
        assertEquals("minecraft:overworld 10.50 64.00 -20.25", location.compact())

        val moved = location.withPosition(0.0, 100.0, 0.0)
        assertEquals(0.0, moved.x)
        assertEquals(100.0, moved.y)
        assertEquals("minecraft:overworld", moved.world)

        val worldChanged = location.withWorld("minecraft:the_nether")
        assertEquals("minecraft:the_nether", worldChanged.world)
    }

    @Test
    fun cellLocation_rejectsInvalid() {
        assertThrows<IllegalArgumentException> {
            CellLocation("   ", 0.0, 0.0, 0.0, 0f, 0f)
        }
        assertThrows<IllegalArgumentException> {
            CellLocation("world", Double.NaN, 0.0, 0.0, 0f, 0f)
        }
    }

    @Test
    fun teleportResult_successAndFailure() {
        val location = CellLocation("minecraft:overworld", 0.0, 64.0, 0.0, 0f, 0f)
        val success = TeleportResult.success(location)
        assertTrue(success.success())
        assertEquals(location, success.destination)

        val failed = TeleportResult.failed(TeleportStatus.UNSAFE_DESTINATION, "error.unsafe")
        assertFalse(failed.success())
        assertNull(failed.destination)
        assertEquals(TeleportStatus.UNSAFE_DESTINATION, failed.status)
    }

    @Test
    fun randomTeleportSettings_validatesRadii() {
        val settings = RandomTeleportSettings(0.0, 0.0, 100, 1000)
        assertEquals(100, settings.minRadius)
        assertEquals(1000, settings.maxRadius)

        assertThrows<IllegalArgumentException> {
            RandomTeleportSettings(0.0, 0.0, 500, 100)
        }
    }

}
