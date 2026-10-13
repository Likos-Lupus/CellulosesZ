package top.likoslupus.cellulosesz.movement.request

import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test
import top.likoslupus.cellulosesz.movement.config.TeleportRequestSettings
import java.util.*
import kotlin.time.Duration.Companion.seconds
import kotlin.time.TestTimeSource

class TeleportRequestServiceTest {

    private val timeSource = TestTimeSource()
    private var settings = TeleportRequestSettings(timeoutSeconds = 60, maxIncomingPerPlayer = 5)
    private val service = TeleportRequestService(
        settings = { settings },
        timeSource = timeSource
    )

    private val sender = UUID.randomUUID()
    private val target = UUID.randomUUID()
    private val other = UUID.randomUUID()

    @Test
    fun `cannot request self`() {
        assertEquals(
            TpaSendResult.Self,
            service.send(
                sender,
                sender,
                TeleportRequestType.TO_TARGET
            )
        )
    }

    @Test
    fun `accept without sender resolves the single request`() {
        service.send(
            sender,
            target,
            TeleportRequestType.TO_TARGET
        )

        val accepted = assertInstanceOf(
            TpaAcceptResult.Accepted::class.java,
            service.accept(target, null)
        )
        assertEquals(
            sender,
            accepted.request.senderId
        )
        assertEquals(
            TeleportRequestType.TO_TARGET,
            accepted.request.type
        )
    }

    @Test
    fun `accept removes the request`() {
        service.send(
            sender,
            target,
            TeleportRequestType.TO_TARGET
        )
        service.accept(target, null)

        assertInstanceOf(
            TpaAcceptResult.None::class.java,
            service.accept(target, null)
        )
    }

    @Test
    fun `multiple pending requests are ambiguous`() {
        service.send(
            sender,
            target,
            TeleportRequestType.TO_TARGET
        )
        service.send(
            other,
            target,
            TeleportRequestType.TO_TARGET
        )

        assertInstanceOf(
            TpaAcceptResult.Ambiguous::class.java,
            service.accept(target, null)
        )
        assertInstanceOf(
            TpaAcceptResult.Accepted::class.java,
            service.accept(target, other)
        )
    }

    @Test
    fun `deny removes the request`() {
        service.send(
            sender,
            target,
            TeleportRequestType.TO_TARGET
        )

        val denied = assertInstanceOf(
            TpaDenyResult.Denied::class.java,
            service.deny(target, null)
        )
        assertEquals(
            sender,
            denied.request.senderId
        )
        assertInstanceOf(
            TpaAcceptResult.None::class.java,
            service.accept(target, null)
        )
    }

    @Test
    fun `cancel removes every outbound request of the sender`() {
        service.send(
            sender,
            target,
            TeleportRequestType.TO_TARGET
        )
        service.send(
            sender,
            other,
            TeleportRequestType.TO_TARGET
        )

        val cancelled = service.cancel(sender)
        assertEquals(
            setOf(target, other),
            cancelled.map { it.targetId }.toSet()
        )
        assertTrue(service.cancel(sender).isEmpty())
    }

    @Test
    fun `expired requests are dropped on access`() {
        service.send(
            sender,
            target,
            TeleportRequestType.TO_TARGET
        )
        timeSource += 61.seconds

        assertInstanceOf(
            TpaAcceptResult.None::class.java,
            service.accept(target, null)
        )
    }

    @Test
    fun `clearPlayer drops outbound and inbound requests`() {
        service.send(
            sender,
            target,
            TeleportRequestType.TO_TARGET
        )
        service.send(
            other,
            sender,
            TeleportRequestType.TO_TARGET
        )

        val affected = service.clearPlayer(sender)

        assertEquals(listOf(other), affected)
        assertTrue(service.cancel(sender).isEmpty())
    }

    @Test
    fun `same request refreshes instead of duplicating`() {
        service.send(
            sender,
            target,
            TeleportRequestType.TO_TARGET
        )
        timeSource += 30.seconds

        val result = service.send(
            sender,
            target,
            TeleportRequestType.TO_TARGET
        )

        assertInstanceOf(
            TpaSendResult.Refreshed::class.java,
            result
        )
        // refreshed request is not expired yet, 60s after the refresh
        timeSource += 30.seconds
        assertInstanceOf(
            TpaAcceptResult.Accepted::class.java,
            service.accept(target, sender)
        )
    }

    @Test
    fun `sender may hold outbound requests to multiple targets`() {
        assertInstanceOf(
            TpaSendResult.Sent::class.java,
            service.send(
                sender,
                target,
                TeleportRequestType.TO_TARGET
            )
        )

        assertInstanceOf(
            TpaSendResult.Sent::class.java,
            service.send(
                sender,
                other,
                TeleportRequestType.TO_TARGET
            )
        )

        assertInstanceOf(
            TpaAcceptResult.Accepted::class.java,
            service.accept(target, sender)
        )
        assertInstanceOf(
            TpaAcceptResult.Accepted::class.java,
            service.accept(other, sender)
        )
    }

    @Test
    fun `per-sender outbound cap is enforced`() {
        settings = settings.copy(maxOutgoingPerPlayer = 1)
        service.send(
            sender,
            target,
            TeleportRequestType.TO_TARGET
        )

        assertEquals(
            TpaSendResult.SenderQueueFull,
            service.send(
                sender,
                other,
                TeleportRequestType.TO_TARGET
            )
        )
    }

    @Test
    fun `server-wide request cap is enforced`() {
        settings = settings.copy(maxRequestsServerWide = 1)
        service.send(
            sender,
            target,
            TeleportRequestType.TO_TARGET
        )

        assertEquals(
            TpaSendResult.ServerQueueFull,
            service.send(
                other,
                target,
                TeleportRequestType.TO_TARGET
            )
        )
    }

    @Test
    fun `target queue limit is enforced`() {
        settings = settings.copy(maxIncomingPerPlayer = 1)
        service.send(
            sender,
            target,
            TeleportRequestType.TO_TARGET
        )

        assertEquals(
            TpaSendResult.TargetQueueFull(target),
            service.send(
                other,
                target,
                TeleportRequestType.TO_TARGET
            )
        )
    }

    @Test
    fun `tpahere direction is preserved`() {
        service.send(
            sender,
            target,
            TeleportRequestType.BRING_TARGET
        )

        val accepted = assertInstanceOf(
            TpaAcceptResult.Accepted::class.java,
            service.accept(target, sender)
        )
        assertEquals(
            TeleportRequestType.BRING_TARGET,
            accepted.request.type
        )
    }

    @Test
    fun `batch filters self and unreachable candidates and honors the batch cap`() {
        settings = settings.copy(maxBatchRecipients = 2)
        val reachable = listOf(other, target)
        val unreachable = UUID.randomUUID()

        val result = assertInstanceOf(
            TpaBatchSendResult.Sent::class.java,
            service.sendBatch(
                sender,
                listOf(sender, other, unreachable, target),
                TeleportRequestType.BRING_TARGET,
            ) { it in reachable },
        )
        assertEquals(listOf(other, target), result.recipients)
    }

    @Test
    fun `batch reports no recipients when every candidate is filtered out`() {
        assertEquals(
            TpaBatchSendResult.NoRecipients,
            service.sendBatch(
                sender,
                listOf(sender),
                TeleportRequestType.BRING_TARGET,
            ) { true },
        )
    }

    @Test
    fun `batch cooldown blocks a second batch until it elapses`() {
        service.sendBatch(
            sender,
            listOf(target),
            TeleportRequestType.BRING_TARGET,
        ) { true }

        assertInstanceOf(
            TpaBatchSendResult.Cooldown::class.java,
            service.sendBatch(
                sender,
                listOf(other),
                TeleportRequestType.BRING_TARGET,
            ) { true },
        )

        timeSource += 121.seconds
        assertInstanceOf(
            TpaBatchSendResult.Sent::class.java,
            service.sendBatch(
                sender,
                listOf(other),
                TeleportRequestType.BRING_TARGET,
            ) { true },
        )
    }

    @Test
    fun `batch stops at the per-sender outbound cap`() {
        settings = settings.copy(maxOutgoingPerPlayer = 1)

        val result = assertInstanceOf(
            TpaBatchSendResult.Sent::class.java,
            service.sendBatch(
                sender,
                listOf(target, other),
                TeleportRequestType.BRING_TARGET,
            ) { true },
        )
        assertEquals(listOf(target), result.recipients)
    }

}
