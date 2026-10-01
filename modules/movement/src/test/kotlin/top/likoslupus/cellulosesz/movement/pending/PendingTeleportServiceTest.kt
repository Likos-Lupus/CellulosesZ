package top.likoslupus.cellulosesz.movement.pending

import kotlinx.coroutines.runBlocking
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test
import top.likoslupus.cellulosesz.movement.config.TeleportSafetySettings
import top.likoslupus.cellulosesz.movement.teleport.SafetyMode
import top.likoslupus.cellulosesz.movement.teleport.StoredPosition
import top.likoslupus.cellulosesz.movement.teleport.TeleportPolicy
import java.util.*
import kotlin.time.Duration

class PendingTeleportServiceTest {

    private val service = PendingTeleportService()
    private val player = UUID.randomUUID()

    private fun policy(
        cancelOnMove: Boolean = true,
        cancelOnDamage: Boolean = true,
        tolerance: Double = 0.15,
    ) =
        TeleportPolicy(
            safetyMode = SafetyMode.FIND_NEAREST_SAFE,
            safety = TeleportSafetySettings(),
            delay = Duration.ZERO,
            cooldown = Duration.ZERO,
            cancelOnMove = cancelOnMove,
            cancelOnDamage = cancelOnDamage,
            movementTolerance = tolerance,
            dismountPassengers = true,
            recordHistory = true,
        )

    private fun position(
        x: Double = 0.0,
        y: Double = 64.0,
        z: Double = 0.0,
        dimension: String = "minecraft:overworld"
    ) =
        StoredPosition(
            dimension,
            x,
            y,
            z,
            0f,
            0f
        )

    @Test
    fun `register and clear track presence`() {
        service.register(player, position(), policy())
        assertTrue(service.has(player))

        service.clear(player)
        assertFalse(service.has(player))
    }

    @Test
    fun `rotation only changes do not cancel`() {
        service.register(player, position(), policy())

        assertNull(
            service.onTick(
                player,
                position().copy(yaw = 180f, pitch = 40f)
            )
        )
    }

    @Test
    fun `movement within tolerance stays pending`() {
        service.register(
            player,
            position(),
            policy(tolerance = 0.5)
        )

        assertNull(
            service.onTick(
                player,
                position(x = 0.2)
            )
        )
        assertTrue(service.has(player))
    }

    @Test
    fun `movement beyond tolerance cancels with MOVED`() = runBlocking {
        val signal = service.register(
            player,
            position(),
            policy(tolerance = 0.15)
        )

        assertEquals(
            TeleportCancellation.MOVED,
            service.onTick(player, position(x = 1.0))
        )
        assertEquals(
            TeleportCancellation.MOVED,
            signal.await()
        )
        assertFalse(service.has(player))
    }

    @Test
    fun `dimension change cancels`() {
        service.register(player, position(), policy())

        assertEquals(
            TeleportCancellation.DIMENSION_CHANGED,
            service.onTick(
                player,
                position(dimension = "minecraft:the_nether")
            ),
        )
    }

    @Test
    fun `cancel on move is skipped when policy disables it`() {
        service.register(
            player,
            position(),
            policy(cancelOnMove = false)
        )

        assertNull(
            service.onTick(
                player,
                position(x = 100.0)
            )
        )
    }

    @Test
    fun `damage cancels when enabled`() = runBlocking {
        val signal = service.register(
            player,
            position(),
            policy(cancelOnDamage = true)
        )

        assertTrue(service.onDamage(player))
        assertEquals(
            TeleportCancellation.DAMAGED,
            signal.await()
        )
    }

    @Test
    fun `damage is ignored when policy disables it`() {
        service.register(
            player,
            position(),
            policy(cancelOnDamage = false)
        )

        assertFalse(service.onDamage(player))
        assertTrue(service.has(player))
    }

    @Test
    fun `cancelAll completes every signal`() = runBlocking {
        val other = UUID.randomUUID()
        val first = service.register(
            player,
            position(),
            policy()
        )
        val second = service.register(
            other,
            position(),
            policy()
        )

        service.cancelAll(TeleportCancellation.SERVER_STOPPING)

        assertEquals(
            TeleportCancellation.SERVER_STOPPING,
            first.await()
        )
        assertEquals(
            TeleportCancellation.SERVER_STOPPING,
            second.await()
        )
        assertFalse(service.has(player))
    }

}
