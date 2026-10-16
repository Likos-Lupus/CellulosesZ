package top.likoslupus.cellulosesz.application.api

import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test
import top.likoslupus.cellulosesz.api.*
import java.util.*
import java.util.concurrent.atomic.AtomicInteger

/**
 * The provider must linearize registration against READY (no lost or duplicated callback) and it must
 * isolate sessions: an API view or event sink obtained in session A never becomes usable again once a
 * session B starts in the same JVM.
 */
class ApiProviderTest {

    private fun api(session: ApiSession): ApplicationApi =
        ApplicationApi(
            version = ApiVersion(0, 1),
            players = ApiPlayerLookup(session.lease) { PlayerIdentity(it, "name") },
            visibility = ApiVisibilityLookup(session.lease) { _, _ -> true },
            teleports = session.teleports,
        )

    private fun event(): TeleportCommitted =
        TeleportCommitted(
            subjectId = UUID.randomUUID(),
            kind = TeleportOriginKind.HOME,
            from = ApiPosition(
                "minecraft:overworld",
                0.0, 0.0, 0.0,
                0f, 0f
            ),
            to = ApiPosition(
                "minecraft:overworld",
                1.0, 2.0, 3.0,
                0f, 0f
            ),
        )

    @Test
    fun `a listener registered before ready is delivered exactly once`() {
        val provider = ApplicationApiProvider()
        val session = provider.beginSession()
        provider.attach(api(session))

        val count = AtomicInteger()
        provider.whenReady { count.incrementAndGet() }
        assertEquals(0, count.get())

        provider.markReady()
        assertEquals(1, count.get())
    }

    @Test
    fun `registering after ready delivers immediately and once`() {
        val provider = ApplicationApiProvider()
        val session = provider.beginSession()
        provider.attach(api(session))
        provider.markReady()

        val count = AtomicInteger()
        provider.whenReady { count.incrementAndGet() }
        assertEquals(1, count.get())
    }

    @Test
    fun `cancel before ready prevents delivery and close is idempotent`() {
        val provider = ApplicationApiProvider()
        val session = provider.beginSession()
        provider.attach(api(session))

        var called = false
        val subscription = provider.whenReady { called = true }
        subscription.close()
        subscription.close()

        provider.markReady()
        assertFalse(called)
    }

    @Test
    fun `a listener registered while stopping is never delivered`() {
        val provider = ApplicationApiProvider()
        val session = provider.beginSession()
        provider.attach(api(session))
        provider.markStopping()

        var called = false
        provider.whenReady { called = true }
        provider.markReady()
        assertFalse(called)
    }

    @Test
    fun `an old session view never revives when a new session is ready`() {
        val provider = ApplicationApiProvider()

        val first = provider.beginSession()
        val firstApi = api(first)
        provider.attach(firstApi)
        provider.markReady()
        provider.markStopping()
        provider.markStopped()

        val second = provider.beginSession()
        val secondApi = api(second)
        provider.attach(secondApi)
        provider.markReady()

        assertTrue(firstApi.players.findById(UUID.randomUUID()) is ApiQuery.Stopping)
        assertTrue(
            firstApi.visibility.canSee(
                UUID.randomUUID(),
                UUID.randomUUID()
            ) is ApiQuery.Stopping
        )
        assertTrue(secondApi.players.findById(UUID.randomUUID()) is ApiQuery.Available)
    }

    @Test
    fun `teleport events are scoped to the ready session`() {
        val provider = ApplicationApiProvider()
        val session = provider.beginSession()
        provider.attach(api(session))
        provider.markReady()

        val delivered = AtomicInteger()
        session.teleports.subscribe { delivered.incrementAndGet() }
        provider.dispatchTeleport(event())
        assertEquals(1, delivered.get())

        provider.markStopping()
        provider.dispatchTeleport(event())
        assertEquals(1, delivered.get())

        val afterClose = AtomicInteger()
        session.teleports.subscribe { afterClose.incrementAndGet() }
        session.teleports.dispatch(event())
        assertEquals(0, afterClose.get())
    }

    @Test
    fun `a throwing listener does not stop the others or the provider`() {
        val provider = ApplicationApiProvider()
        val session = provider.beginSession()
        provider.attach(api(session))

        val ran = AtomicInteger()
        provider.whenReady { throw IllegalStateException("boom") }
        provider.whenReady { ran.incrementAndGet() }

        provider.markReady()
        assertEquals(1, ran.get())
        assertEquals(ApiAvailability.READY, provider.availability())
    }

}
