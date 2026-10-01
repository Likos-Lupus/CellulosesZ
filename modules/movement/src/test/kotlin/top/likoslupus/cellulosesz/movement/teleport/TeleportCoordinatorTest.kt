package top.likoslupus.cellulosesz.movement.teleport

import kotlinx.coroutines.runBlocking
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertInstanceOf
import org.junit.jupiter.api.Test
import top.likoslupus.cellulosesz.movement.config.TeleportSafetySettings
import top.likoslupus.cellulosesz.movement.pending.PendingTeleportService
import top.likoslupus.cellulosesz.movement.teleport.cooldown.TeleportCooldowns
import top.likoslupus.cellulosesz.movement.teleport.history.TeleportHistoryRepository
import top.likoslupus.cellulosesz.movement.teleport.history.TeleportHistoryService
import java.util.*
import kotlin.time.Duration
import kotlin.time.Duration.Companion.seconds
import kotlin.time.TestTimeSource

class TeleportCoordinatorTest {

    private val subject = UUID.randomUUID()
    private val target = UUID.randomUUID()

    private val backend = FakeBackend()
    private val historyRepository = FakeHistoryRepository()
    private val timeSource = TestTimeSource()
    private val coordinator = TeleportCoordinator(
        backend = backend,
        pending = PendingTeleportService(),
        cooldowns = TeleportCooldowns(timeSource),
        history = TeleportHistoryService(historyRepository),
        historyEnabled = { true },
    )

    private fun policy(
        cooldown: Duration = Duration.ZERO,
        safetyMode: SafetyMode = SafetyMode.ALLOW_UNSAFE
    ) =
        TeleportPolicy(
            safetyMode = safetyMode,
            safety = TeleportSafetySettings(),
            delay = Duration.ZERO,
            cooldown = cooldown,
            cancelOnMove = true,
            cancelOnDamage = true,
            movementTolerance = 0.15,
            dismountPassengers = true,
            recordHistory = true,
        )

    private fun position(y: Double = 64.0) =
        StoredPosition(
            dimension = "minecraft:overworld",
            x = 1.0,
            y = y,
            z = 2.0,
            yaw = 0f,
            pitch = 0f
        )

    @Test
    fun `successful fixed teleport records history and returns a receipt`(): Unit =
        runBlocking {
            backend.positions[subject] = position()
            val destination = position(y = 70.0)
            backend.moveResult = BackendMoveResult.Success(destination)

            val outcome = coordinator.execute(
                TeleportIntent(
                    subject,
                    TeleportDestination.Fixed(destination),
                    TeleportCause.HOME,
                    policy()
                )
            )

            val success = assertInstanceOf(
                TeleportOutcome.Success::class.java,
                outcome
            )
            assertEquals(subject, success.receipt.subjectId)
            assertEquals(destination, success.receipt.destination)
            assertEquals(position(), historyRepository.entries[subject])
        }

    @Test
    fun `player destination is resolved at commit time`(): Unit =
        runBlocking {
            backend.positions[subject] = position()
            backend.positions[target] = position(y = 80.0)
            backend.moveResult = BackendMoveResult.Success(position(y = 80.0))

            val outcome = coordinator.execute(
                TeleportIntent(
                    subject,
                    TeleportDestination.Player(target),
                    TeleportCause.REQUEST,
                    policy(),
                )
            )

            assertInstanceOf(
                TeleportOutcome.Success::class.java,
                outcome
            )
            assertEquals(
                position(y = 80.0),
                backend.lastDestination
            )
        }

    @Test
    fun `offline target fails without moving`(): Unit =
        runBlocking {
            backend.positions[subject] = position()

            val outcome = coordinator.execute(
                TeleportIntent(
                    subject,
                    TeleportDestination.Player(target),
                    TeleportCause.REQUEST,
                    policy()
                )
            )

            assertEquals(TeleportOutcome.TargetOffline, outcome)
        }

    @Test
    fun `cooldown blocks a second teleport`(): Unit =
        runBlocking {
            backend.positions[subject] = position()
            backend.moveResult = BackendMoveResult.Success(position(y = 70.0))
            val intent = TeleportIntent(
                subject,
                TeleportDestination.Fixed(position(y = 70.0)),
                TeleportCause.HOME,
                policy(cooldown = 10.seconds),
            )

            assertInstanceOf(
                TeleportOutcome.Success::class.java,
                coordinator.execute(intent)
            )
            assertInstanceOf(
                TeleportOutcome.Cooldown::class.java,
                coordinator.execute(intent)
            )
        }

    @Test
    fun `backend player offline maps to outcome`(): Unit =
        runBlocking {
            backend.positions[subject] = position()
            backend.moveResult = BackendMoveResult.PlayerOffline

            val outcome = coordinator.execute(
                TeleportIntent(
                    subject,
                    TeleportDestination.Fixed(position()),
                    TeleportCause.HOME,
                    policy()
                )
            )

            assertEquals(TeleportOutcome.PlayerOffline, outcome)
        }

    private class FakeBackend : TeleportBackend {

        val positions = mutableMapOf<UUID, StoredPosition>()
        var moveResult: BackendMoveResult = BackendMoveResult.PlayerOffline
        var lastDestination: StoredPosition? = null

        override suspend fun position(playerId: UUID): StoredPosition? = positions[playerId]

        override suspend fun vanillaSpawnPosition(): StoredPosition =
            StoredPosition(
                dimension = "minecraft:overworld",
                x = 0.0,
                y = 64.0,
                z = 0.0,
                yaw = 0f,
                pitch = 0f
            )

        override suspend fun move(
            playerId: UUID,
            destination: StoredPosition,
            policy: TeleportPolicy,
        ): BackendMoveResult {
            lastDestination = destination
            return moveResult
        }

    }

    private class FakeHistoryRepository : TeleportHistoryRepository {

        val entries = mutableMapOf<UUID, StoredPosition>()

        override suspend fun read(playerId: UUID): StoredPosition? =
            entries[playerId]

        override suspend fun write(
            playerId: UUID,
            position: StoredPosition
        ) {
            entries[playerId] = position
        }

    }

}
