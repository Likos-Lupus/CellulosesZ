package top.likoslupus.cellulosesz.administration.integration

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import java.util.*
import java.util.concurrent.atomic.AtomicInteger

class ModerationEventBusTest {

    @Test
    fun `publishes to every observer and isolates a throwing one`() {
        val bus = ModerationEventBus()
        val received = mutableListOf<ModerationDomainEvent>()
        val calls = AtomicInteger()
        bus.subscribe { received += it }
        bus.subscribe { throw IllegalStateException("boom") }
        bus.subscribe {
            received += it
            calls.incrementAndGet()
        }

        val event = ModerationDomainEvent.Unmuted(
            UUID.randomUUID(),
            "action-1"
        )
        bus.publish(event)

        assertEquals(listOf(event, event), received)
        assertEquals(1, calls.get())
    }

    @Test
    fun `a closed registration stops receiving`() {
        val bus = ModerationEventBus()
        val received = AtomicInteger()
        val registration = bus.subscribe { received.incrementAndGet() }

        registration.close()
        bus.publish(
            ModerationDomainEvent.Released(
                UUID.randomUUID(),
                "action-2"
            )
        )

        assertEquals(0, received.get())
    }

}
