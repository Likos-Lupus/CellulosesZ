package top.likoslupus.cellulosesz.tpa

import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test
import java.time.Duration
import java.time.Instant
import java.util.*

class TeleportRequestServiceTest {

    private var now: Instant = Instant.parse("2026-01-01T00:00:00Z")
    private val service = TeleportRequestService(
        timeout = { Duration.ofSeconds(60) },
        clock = { now }
    )

    private val sender = UUID.randomUUID()
    private val target = UUID.randomUUID()
    private val other = UUID.randomUUID()

    @Test
    fun `cannot request self`() {
        assertEquals(
            TpaSendResult.Self,
            service.send(sender, sender)
        )
    }

    @Test
    fun `accept without sender resolves the single request`() {
        service.send(sender, target)

        val result = service.accept(target, null)

        val accepted = assertInstanceOf(
            TpaAcceptResult.Accepted::class.java,
            result
        )
        assertEquals(
            sender,
            accepted.request.senderId
        )
    }

    @Test
    fun `accept removes the request`() {
        service.send(sender, target)
        service.accept(target, null)

        assertInstanceOf(
            TpaAcceptResult.None::class.java,
            service.accept(target, null)
        )
    }

    @Test
    fun `multiple pending requests are ambiguous`() {
        service.send(sender, target)
        service.send(other, target)

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
        service.send(sender, target)

        val result = service.deny(target, null)

        val denied = assertInstanceOf(
            TpaDenyResult.Denied::class.java,
            result
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
        service.send(sender, target)

        assertEquals(
            target,
            service.cancel(sender)?.targetId
        )
        assertNull(service.cancel(sender))
    }

    @Test
    fun `expired requests are dropped on access`() {
        service.send(sender, target)
        now = now.plusSeconds(61)

        assertInstanceOf(
            TpaAcceptResult.None::class.java,
            service.accept(target, null)
        )
    }

    @Test
    fun `clearPlayer drops outbound and inbound requests`() {
        service.send(sender, target)
        service.send(other, sender)

        val affected = service.clearPlayer(sender)

        assertEquals(listOf(other), affected)
        assertNull(service.cancel(sender))
    }

}
