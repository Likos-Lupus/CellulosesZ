package top.likoslupus.cellulosesz.communication.lifecycle

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test

class DeathCoordinatesTest {

    @Test
    fun `floors coordinates to block positions`() {
        assertEquals("12", DeathCoordinates.coordinate(12.9))
        assertEquals("-4", DeathCoordinates.coordinate(-3.1))
        assertEquals("0", DeathCoordinates.coordinate(0.0))
    }

    @Test
    fun `resolves the world and coordinate tokens`() {
        assertEquals(
            "minecraft:overworld 1 64 -4",
            DeathCoordinates.resolve(
                "{WORLD} {X} {Y} {Z}",
                "minecraft:overworld",
                1.9,
                64.0,
                -3.1,
            ),
        )
    }

}
