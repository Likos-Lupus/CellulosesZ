package top.likoslupus.cellulosesz.movement.teleport

import org.junit.jupiter.api.Assertions.assertThrows
import org.junit.jupiter.api.Test

class StoredPositionTest {

    @Test
    fun `accepts finite coordinates`() {
        StoredPosition(
            dimension = "minecraft:overworld",
            x = 1.0,
            y = 2.0,
            z = 3.0,
            yaw = 0f,
            pitch = 0f
        )
    }

    @Test
    fun `rejects non finite coordinate`() {
        assertThrows(IllegalArgumentException::class.java) {
            StoredPosition(
                dimension = "minecraft:overworld",
                x = Double.NaN,
                y = 2.0,
                z = 3.0,
                yaw = 0f,
                pitch = 0f
            )
        }
        assertThrows(IllegalArgumentException::class.java) {
            StoredPosition(
                dimension = "minecraft:overworld",
                x = 1.0,
                y = Double.POSITIVE_INFINITY,
                z = 3.0,
                yaw = 0f,
                pitch = 0f
            )
        }
    }

    @Test
    fun `rejects non finite rotation`() {
        assertThrows(IllegalArgumentException::class.java) {
            StoredPosition(
                dimension = "minecraft:overworld",
                x = 1.0,
                y = 2.0,
                z = 3.0,
                yaw = Float.NaN,
                pitch = 0f
            )
        }
    }

}
