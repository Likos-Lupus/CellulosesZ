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
    fun `cancel removes the senders outbound request`() {
        service.send(
            sender,
            target,
            TeleportRequestType.TO_TARGET
        )

        assertEquals(
            target,
            service.cancel(sender)?.targetId
        )
        assertNull(service.cancel(sender))
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
        assertNull(service.cancel(sender))
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
    fun `sender cannot silently replace an outbound request to another target`() {
        service.send(
            sender,
            target,
            TeleportRequestType.TO_TARGET
        )

        assertEquals(
            TpaSendResult.SenderAlreadyHasRequest,
            service.send(
                sender,
                other,
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
            TpaSendResult.TargetQueueFull,
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

}
