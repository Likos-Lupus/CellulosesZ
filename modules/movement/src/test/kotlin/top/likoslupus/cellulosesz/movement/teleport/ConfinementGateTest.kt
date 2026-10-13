package top.likoslupus.cellulosesz.movement.teleport

import kotlinx.coroutines.runBlocking
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test
import top.likoslupus.cellulosesz.movement.config.TeleportSafetySettings
import top.likoslupus.cellulosesz.movement.pending.PendingTeleportService
import top.likoslupus.cellulosesz.movement.teleport.cooldown.TeleportCooldowns
import top.likoslupus.cellulosesz.movement.teleport.history.TeleportHistoryRepository
import top.likoslupus.cellulosesz.movement.teleport.history.TeleportHistoryService
import java.util.*
import kotlin.time.Duration

class ConfinementGateTest {

    private val subject = UUID.randomUUID()
    private val backend = FakeBackend()

    private val coordinator = TeleportCoordinator(
        backend = backend,
        pending = PendingTeleportService(),
        cooldowns = TeleportCooldowns(),
        history = TeleportHistoryService(FakeHistoryRepository()),
        historyEnabled = { false },
        confinement = { it == subject },
    )

    private fun position(y: Double = 64.0) =
        StoredPosition(
            dimension = "minecraft:overworld",
            x = 1.0,
            y = y,
            z = 2.0,
            yaw = 0f,
            pitch = 0f,
        )

    private fun policy() =
        TeleportPolicy(
            safetyMode = SafetyMode.ALLOW_UNSAFE,
            safety = TeleportSafetySettings(),
            delay = Duration.ZERO,
            cooldown = Duration.ZERO,
            cancelOnMove = true,
            cancelOnDamage = true,
            movementTolerance = 0.15,
            dismountPassengers = true,
            recordHistory = false,
        )

    @Test
    fun `a confined player is denied a player-instigated teleport`() =
        runBlocking {
            backend.positions[subject] = position()
            backend.moveResult = BackendMoveResult.Success(position(y = 70.0))

            val outcome = coordinator.execute(
                TeleportIntent(
                    subject,
                    TeleportDestination.Fixed(position(y = 70.0)),
                    TeleportCause.HOME,
                    policy(),
                )
            )

            assertEquals(TeleportOutcome.Confined, outcome)
            assertFalse(
                backend.moveCalled,
                "a confined player must not be moved"
            )
        }

    @Test
    fun `administrative and jail causes bypass the confinement gate`() =
        runBlocking {
            backend.positions[subject] = position()
            backend.moveResult = BackendMoveResult.Success(position(y = 70.0))

            assertInstanceOf(
                TeleportOutcome.Success::class.java,
                coordinator.execute(
                    TeleportIntent(
                        subject,
                        TeleportDestination.Fixed(position(y = 70.0)),
                        TeleportCause.ADMIN,
                        policy(),
                    )
                )
            )
            assertInstanceOf(
                TeleportOutcome.Success::class.java,
                coordinator.execute(
                    TeleportIntent(
                        subject,
                        TeleportDestination.Fixed(position(y = 70.0)),
                        TeleportCause.JAIL_CONFINEMENT,
                        policy(),
                    )
                )
            )
            assertTrue(backend.moveCalled)
        }

    private class FakeBackend : TeleportBackend {

        val positions = mutableMapOf<UUID, StoredPosition>()
        var moveResult: BackendMoveResult = BackendMoveResult.PlayerOffline
        var moveCalled: Boolean = false

        override suspend fun position(playerId: UUID): StoredPosition? = positions[playerId]

        override suspend fun vanillaSpawnPosition(): StoredPosition =
            StoredPosition(
                dimension = "minecraft:overworld",
                x = 0.0,
                y = 64.0,
                z = 0.0,
                yaw = 0f,
                pitch = 0f,
            )

        override suspend fun move(
            playerId: UUID,
            destination: StoredPosition,
            policy: TeleportPolicy,
        ): BackendMoveResult {
            moveCalled = true
            return moveResult
        }

    }

    private class FakeHistoryRepository : TeleportHistoryRepository {

        override suspend fun read(playerId: UUID): StoredPosition? =
            null

        override suspend fun write(playerId: UUID, position: StoredPosition) =
            Unit

    }

}
