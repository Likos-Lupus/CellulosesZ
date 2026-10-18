package top.likoslupus.cellulosesz.movement.location

import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test
import top.likoslupus.cellulosesz.movement.teleport.StoredPosition
import java.time.Instant
import java.util.*
import kotlinx.coroutines.runBlocking

class LastLocationServiceTest {

    private val player = UUID.randomUUID()
    private val at: Instant = Instant.parse("2026-01-01T00:00:00Z")

    private val repository = FakeRepository()
    private val service = LastLocationService(repository) { at }

    private fun position(x: Double) =
        StoredPosition(
            dimension = "minecraft:overworld",
            x = x,
            y = 64.0,
            z = 2.0,
            yaw = 45f,
            pitch = 10f,
        )

    @Test
    fun `persist serializes the cached position and clears the cache`() =
        runBlocking {
            service.remember(player, position(7.0))

            service.persist(player)

            val written = repository.writes.getValue(player)
            assertEquals(position(7.0), written.position)
            assertEquals(at, written.updatedAt)

            // The entry was dropped, so the next read must come from storage.
            assertEquals(
                position(7.0),
                service.lastLocation(player)?.position
            )
        }

    @Test
    fun `the live cache wins over storage`() =
        runBlocking {
            service.remember(player, position(1.0))
            service.remember(player, position(2.0))

            assertEquals(
                2.0,
                service.lastLocation(player)?.position?.x
            )
        }

    @Test
    fun `a read falls back to storage when nothing is cached`() =
        runBlocking {
            repository.writes[player] = LastLocation(position(9.0), at)

            val read = service.lastLocation(player)

            assertEquals(9.0, read?.position?.x)
            assertEquals(at, read?.updatedAt)
        }

    @Test
    fun `persist without a cached position is a no-op`() =
        runBlocking {
            service.persist(player)

            assertTrue(repository.writes.isEmpty())
        }

    @Test
    fun `a read of an unknown player is null`() =
        runBlocking {
            assertNull(service.lastLocation(UUID.randomUUID()))
        }

    private class FakeRepository : LastLocationRepository {

        val writes = mutableMapOf<UUID, LastLocation>()

        override suspend fun write(
            playerId: UUID,
            position: StoredPosition,
            updatedAt: Instant,
        ) {
            writes[playerId] = LastLocation(position, updatedAt)
        }

        override suspend fun read(playerId: UUID): LastLocation? =
            writes[playerId]

    }

}
