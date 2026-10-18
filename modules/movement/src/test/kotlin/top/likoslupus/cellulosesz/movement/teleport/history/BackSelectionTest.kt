package top.likoslupus.cellulosesz.movement.teleport.history

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Test
import top.likoslupus.cellulosesz.movement.teleport.StoredPosition

class BackSelectionTest {

    private fun position(x: Double) =
        StoredPosition(
            dimension = "minecraft:overworld",
            x = x,
            y = 64.0,
            z = 0.0,
            yaw = 0f,
            pitch = 0f,
        )

    @Test
    fun `prefers the death point when recording is on and the permission is held`() {
        val teleport = position(1.0)
        val death = position(2.0)

        assertEquals(
            death,
            BackSelection.select(
                teleport,
                death,
                recordDeath = true,
                hasDeathPermission = true
            ),
        )
    }

    @Test
    fun `falls back to the teleport point without the death permission`() {
        val teleport = position(1.0)

        assertEquals(
            teleport,
            BackSelection.select(
                teleport,
                position(2.0),
                recordDeath = true,
                hasDeathPermission = false,
            ),
        )
    }

    @Test
    fun `falls back to the teleport point when death recording is disabled`() {
        val teleport = position(1.0)

        assertEquals(
            teleport,
            BackSelection.select(
                teleport,
                position(2.0),
                recordDeath = false,
                hasDeathPermission = true,
            ),
        )
    }

    @Test
    fun `falls back to the teleport point when no death row exists`() {
        val teleport = position(1.0)

        assertEquals(
            teleport,
            BackSelection.select(
                teleport,
                death = null,
                recordDeath = true,
                hasDeathPermission = true,
            ),
        )
    }

    @Test
    fun `returns null when there is nothing to return to`() {
        assertNull(
            BackSelection.select(
                teleport = null,
                death = null,
                recordDeath = true,
                hasDeathPermission = true,
            )
        )
    }

}
