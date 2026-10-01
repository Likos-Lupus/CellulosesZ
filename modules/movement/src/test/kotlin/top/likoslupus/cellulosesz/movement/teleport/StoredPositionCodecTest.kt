package top.likoslupus.cellulosesz.movement.teleport

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import top.likoslupus.cellulosesz.foundation.persistence.StorageJson

class StoredPositionCodecTest {

    @Test
    fun `round trips through the storage codec`() {
        val position = StoredPosition(
            dimension = "minecraft:overworld",
            x = 12.5,
            y = 64.0,
            z = -3.25,
            yaw = 90f,
            pitch = -45f
        )

        val decoded = StorageJson.format.decodeFromString(
            StoredPosition.serializer(),
            StorageJson.format.encodeToString(StoredPosition.serializer(), position),
        )

        assertEquals(position, decoded)
    }

}
