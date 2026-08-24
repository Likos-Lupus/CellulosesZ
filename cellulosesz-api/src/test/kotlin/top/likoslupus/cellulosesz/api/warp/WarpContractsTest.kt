package top.likoslupus.cellulosesz.api.warp

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import top.likoslupus.cellulosesz.api.teleport.CellLocation
import java.math.BigDecimal
import java.util.*

class WarpContractsTest {

    @Test
    fun warp_createsWithDefaults() {
        val location = CellLocation(
            world = "minecraft:overworld",
            x = 0.0, y = 64.0, z = 0.0,
            yaw = 0f, pitch = 0f
        )
        val warp = Warp("spawn", location)
        assertEquals("spawn", warp.name)
        assertEquals("spawn", warp.displayName)
        assertEquals(BigDecimal.ZERO, warp.cost)
        assertEquals(location, warp.location)
        assertNull(warp.createdBy)
    }

    @Test
    fun warp_withCreator() {
        val location = CellLocation(
            world = "minecraft:overworld",
            x = 10.0, y = 64.0, z = 10.0,
            yaw = 0f, pitch = 0f
        )
        val creator = UUID.randomUUID()
        val warp = Warp(
            name = "shop",
            displayName = "Server Shop",
            cost = BigDecimal("50.00"),
            location = location,
            createdBy = creator,
            createdAt = java.time.Instant.now()
        )
        assertEquals("shop", warp.name)
        assertEquals("Server Shop", warp.displayName)
        assertEquals(BigDecimal("50.00"), warp.cost)
        assertEquals(creator, warp.createdBy)
    }

    @Test
    fun warp_rejectsNegativeCost() {
        val location = CellLocation(
            world = "minecraft:overworld",
            x = 0.0, y = 64.0, z = 0.0,
            yaw = 0f, pitch = 0f
        )
        assertThrows<IllegalArgumentException> {
            Warp(
                name = "bad",
                displayName = "Bad",
                cost = BigDecimal("-1.0"),
                location = location,
                createdBy = null,
                createdAt = java.time.Instant.now()
            )
        }
    }

}
