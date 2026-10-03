package top.likoslupus.cellulosesz.communication.messaging

import net.minecraft.network.chat.Component
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import top.likoslupus.cellulosesz.communication.CommunicationIntegration
import top.likoslupus.cellulosesz.communication.PrivateMessageObservation
import top.likoslupus.cellulosesz.communication.SendGateResult
import top.likoslupus.cellulosesz.communication.config.PrivateMessageSettings
import top.likoslupus.cellulosesz.communication.config.ReplyMode
import java.util.*

class PrivateMessageServiceTest {

    private val aliceId = UUID.randomUUID()
    private val bobId = UUID.randomUUID()
    private val alice = OnlinePlayerIdentity(aliceId, "Alice")
    private val bob = OnlinePlayerIdentity(bobId, "Bob")

    private val backend = FakeBackend()
    private val recipients = FakeRecipients()
    private val replyState = ReplyState(timeout = { null })
    private val observations = mutableListOf<PrivateMessageObservation>()
    private var gate: (UUID) -> SendGateResult = { SendGateResult.Allowed }
    private var reachable: (UUID, UUID) -> Boolean = { _, _ -> true }
    private var settings = PrivateMessageSettings()

    private val service = PrivateMessageService(
        backend = backend,
        recipients = recipients,
        replyState = replyState,
        settings = { settings },
        integration = {
            CommunicationIntegration(
                senderGate = { gate(it) },
                targetReachability = { a, b -> reachable(a, b) },
                observer = { observations += it },
            )
        },
    )

    init {
        backend.byId[aliceId] = alice
        backend.byId[bobId] = bob
        backend.byName["alice"] = alice
        backend.byName["bob"] = bob
    }

    @Test
    fun `delivers and records reply state and observation`() {
        val outcome = service.send(
            aliceId,
            "Bob",
            "hello"
        )

        assertTrue(outcome is PrivateMessageOutcome.Delivered)
        assertEquals(listOf(bobId), backend.delivered)
        assertEquals(1, observations.size)
        assertEquals("hello", observations.single().text)
        assertEquals(
            bobId,
            replyState.resolve(
                aliceId,
                ReplyMode.LAST_INTERACTION
            )?.playerId
        )
        assertEquals(
            aliceId,
            replyState.resolve(
                bobId,
                ReplyMode.LAST_INCOMING
            )?.playerId
        )
    }

    @Test
    fun `disabled messaging short circuits`() {
        settings = settings.copy(enabled = false)

        assertEquals(
            PrivateMessageOutcome.Disabled,
            service.send(
                aliceId,
                "Bob",
                "hi"
            )
        )
    }

    @Test
    fun `rejects invalid body`() {
        assertEquals(
            PrivateMessageOutcome.InvalidMessage,
            service.send(
                aliceId,
                "Bob",
                "   "
            )
        )
    }

    @Test
    fun `denied sender is restricted and never resolved`() {
        gate = { SendGateResult.Denied(Component.literal("muted")) }

        val outcome = service.send(
            aliceId,
            "Bob",
            "hi"
        )

        assertTrue(outcome is PrivateMessageOutcome.SenderRestricted)
        assertTrue(backend.delivered.isEmpty())
    }

    @Test
    fun `unknown target is offline`() {
        val outcome = service.send(
            aliceId,
            "Carol",
            "hi"
        )

        assertTrue(outcome is PrivateMessageOutcome.TargetOffline)
    }

    @Test
    fun `unreachable target is reported as offline`() {
        reachable = { _, _ -> false }

        val outcome = service.send(
            aliceId,
            "Bob",
            "hi"
        )

        assertTrue(outcome is PrivateMessageOutcome.TargetOffline)
        assertTrue(backend.delivered.isEmpty())
    }

    @Test
    fun `self target rejected`() {
        assertEquals(
            PrivateMessageOutcome.SelfTarget,
            service.send(
                aliceId,
                "Alice",
                "hi"
            )
        )
    }

    @Test
    fun `loading recipient reported`() {
        recipients.decision = ReceiveDecision.LOADING

        assertEquals(
            PrivateMessageOutcome.RecipientStateLoading,
            service.send(
                aliceId,
                "Bob",
                "hi"
            )
        )
    }

    @Test
    fun `denied recipient reported as unavailable`() {
        recipients.decision = ReceiveDecision.DENIED

        assertEquals(
            PrivateMessageOutcome.RecipientUnavailable,
            service.send(
                aliceId,
                "Bob",
                "hi"
            )
        )
    }

    @Test
    fun `failed delivery does not mutate reply state or observe`() {
        backend.deliverResult = false

        val outcome = service.send(
            aliceId,
            "Bob",
            "hi"
        )

        assertTrue(outcome is PrivateMessageOutcome.TargetOffline)
        assertEquals(
            null,
            replyState.resolve(
                aliceId,
                ReplyMode.LAST_INTERACTION
            )
        )
        assertTrue(observations.isEmpty())
    }

    @Test
    fun `observer failure does not fail delivery`() {
        val throwing = PrivateMessageService(
            backend = backend,
            recipients = recipients,
            replyState = ReplyState(timeout = { null }),
            settings = { settings },
            integration = {
                CommunicationIntegration(observer = { throw IllegalStateException("boom") })
            },
        )

        val outcome = throwing.send(
            aliceId,
            "Bob",
            "hello"
        )

        assertTrue(outcome is PrivateMessageOutcome.Delivered)
    }

    @Test
    fun `reply without a target is rejected`() {
        assertEquals(
            PrivateMessageOutcome.NoReplyTarget,
            service.reply(
                aliceId,
                "hi"
            )
        )
    }

    @Test
    fun `reply after a delivery reaches the counterpart`() {
        service.send(
            aliceId,
            "Bob",
            "hello"
        )

        val outcome = service.reply(
            bobId,
            "hi back"
        )

        assertTrue(outcome is PrivateMessageOutcome.Delivered)
        assertEquals(listOf(bobId, aliceId), backend.delivered)
    }

    private class FakeBackend : MessagingBackend {

        val byId = HashMap<UUID, OnlinePlayerIdentity>()
        val byName = HashMap<String, OnlinePlayerIdentity>()
        val delivered = mutableListOf<UUID>()
        var deliverResult = true

        override fun onlineIdentityByName(name: String): OnlinePlayerIdentity? =
            byName[name.lowercase()]

        override fun onlineIdentityById(id: UUID): OnlinePlayerIdentity? =
            byId[id]

        override fun deliverPrivateMessage(playerId: UUID, message: Component): Boolean {
            if (!deliverResult) {
                return false
            }
            delivered += playerId
            return true
        }

    }

    private class FakeRecipients : RecipientPolicy {

        var decision: ReceiveDecision = ReceiveDecision.ALLOWED
        var mode: ReplyMode = ReplyMode.LAST_INTERACTION

        override fun canReceiveFrom(
            recipientId: UUID,
            senderId: UUID
        ): ReceiveDecision =
            decision

        override fun replyMode(playerId: UUID): ReplyMode = mode

    }

}
