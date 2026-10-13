package top.likoslupus.cellulosesz.communication.action

import kotlinx.coroutines.runBlocking
import net.kyori.adventure.text.Component
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test
import top.likoslupus.cellulosesz.communication.*
import top.likoslupus.cellulosesz.communication.config.ActionAudience
import top.likoslupus.cellulosesz.communication.config.ActionMessageSettings
import top.likoslupus.cellulosesz.communication.config.ReplyMode
import top.likoslupus.cellulosesz.communication.messaging.OnlinePlayerIdentity
import top.likoslupus.cellulosesz.communication.messaging.ReceiveDecision
import top.likoslupus.cellulosesz.communication.messaging.RecipientPolicy
import java.util.*

class ActionMessageServiceTest {

    private val sender = UUID.randomUUID()
    private val sameDimension = OnlinePlayerIdentity(UUID.randomUUID(), "Same")
    private val otherDimension = OnlinePlayerIdentity(UUID.randomUUID(), "Other")

    private val directory = FakeDirectory()
    private var settings = ActionMessageSettings(messagesPerMinute = 2)

    private var muted = false
    private val ignored = mutableSetOf<UUID>()
    private var reachable: (UUID, UUID) -> Boolean = { _, _ -> true }

    private val policy = object : RecipientPolicy {
        override fun canReceiveFrom(
            recipientId: UUID,
            senderId: UUID,
        ): ReceiveDecision = ReceiveDecision.ALLOWED

        override fun replyMode(playerId: UUID): ReplyMode =
            ReplyMode.LAST_INTERACTION

        override fun ignores(ignorerId: UUID, targetId: UUID): Boolean =
            targetId in ignored
    }

    private val integration = CommunicationIntegration(
        senderGate = {
            when {
                muted -> SendGateResult.Denied(SenderRestriction.MUTED)
                else -> SendGateResult.Allowed
            }
        },
        targetReachability = { senderId, targetId -> reachable(senderId, targetId) },
    )

    private val service = ActionMessageService(
        runner = ImmediateServerThreadRunner,
        directory = directory,
        presentation = FakePresentation(),
        recipients = policy,
        integration = { integration },
        settings = { settings },
        timeSource = FakeTimeSource(),
    )

    @Test
    fun `delivers to the sender's dimension only`() =
        runBlocking {
            directory.onlineInResult += sameDimension
            directory.onlineResult += sameDimension
            directory.onlineResult += otherDimension

            val result = service.emit(
                sender,
                "Alice",
                "minecraft:overworld",
                "waves"
            )

            assertInstanceOf(
                ActionMessageResult.Delivered::class.java,
                result
            )
            assertEquals(1, directory.sent.size)
        }

    @Test
    fun `rate limits per sender`() =
        runBlocking {
            assertInstanceOf(
                ActionMessageResult.Delivered::class.java,
                service.emit(
                    sender,
                    "Alice",
                    null,
                    "one"
                ),
            )
            assertInstanceOf(
                ActionMessageResult.Delivered::class.java,
                service.emit(
                    sender,
                    "Alice",
                    null,
                    "two"
                ),
            )
            assertEquals(
                ActionMessageResult.RateLimited,
                service.emit(
                    sender,
                    "Alice",
                    null,
                    "three"
                ),
            )
        }

    @Test
    fun `rejects empty and too long actions`() =
        runBlocking {
            assertEquals(
                ActionMessageResult.InvalidMessage,
                service.emit(
                    sender,
                    "Alice",
                    null,
                    "   "
                ),
            )

            settings = settings.copy(maximumLength = 4)
            assertEquals(
                ActionMessageResult.InvalidMessage,
                service.emit(
                    sender,
                    "Alice",
                    null,
                    "far too long"
                ),
            )
        }

    @Test
    fun `rejects when disabled`() =
        runBlocking {
            settings = settings.copy(enabled = false)

            assertEquals(
                ActionMessageResult.Disabled,
                service.emit(
                    sender,
                    "Alice",
                    null,
                    "waves"
                ),
            )
        }

    @Test
    fun `server audience reaches every online player`() =
        runBlocking {
            settings = settings.copy(audience = ActionAudience.SERVER)
            directory.onlineResult += sameDimension
            directory.onlineResult += otherDimension

            val result = service.emit(
                sender,
                "Alice",
                "minecraft:overworld",
                "waves"
            )

            assertTrue(result is ActionMessageResult.Delivered)
            assertEquals(2, directory.sent.size)
        }

    @Test
    fun `a muted sender is rejected`() =
        runBlocking {
            muted = true

            assertEquals(
                ActionMessageResult.Muted,
                service.emit(
                    sender,
                    "Alice",
                    null,
                    "waves"
                ),
            )
            assertTrue(directory.sent.isEmpty())
        }

    @Test
    fun `recipients the sender ignores are filtered`() =
        runBlocking {
            directory.onlineInResult += sameDimension
            ignored += sameDimension.id

            val result = service.emit(
                sender,
                "Alice",
                "minecraft:overworld",
                "waves"
            )

            assertTrue(result is ActionMessageResult.Delivered)
            assertTrue(directory.sent.isEmpty())
        }

    @Test
    fun `recipients the sender cannot see are filtered`() =
        runBlocking {
            directory.onlineInResult += sameDimension
            reachable = { _, _ -> false }

            val result = service.emit(
                sender,
                "Alice",
                "minecraft:overworld",
                "waves"
            )

            assertTrue(result is ActionMessageResult.Delivered)
            assertTrue(directory.sent.isEmpty())
        }

    private class FakeDirectory : PlayerDirectory {

        val onlineResult = mutableListOf<OnlinePlayerIdentity>()
        val onlineInResult = mutableListOf<OnlinePlayerIdentity>()
        val sent = mutableListOf<Pair<UUID, Component>>()

        override fun online(): List<OnlinePlayerIdentity> =
            onlineResult

        override fun moderators(): List<OnlinePlayerIdentity> =
            emptyList()

        override fun onlineIn(dimensionId: String): List<OnlinePlayerIdentity> =
            onlineInResult

        override fun send(playerId: UUID, message: Component): Boolean {
            sent += playerId to message
            return true
        }

    }

}
