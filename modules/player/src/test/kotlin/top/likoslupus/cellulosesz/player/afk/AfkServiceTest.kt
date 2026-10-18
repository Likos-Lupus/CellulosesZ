package top.likoslupus.cellulosesz.player.afk

import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test
import top.likoslupus.cellulosesz.core.player.SleepExclusionRegistry
import top.likoslupus.cellulosesz.player.config.AfkSettings
import java.time.Clock
import java.time.Instant
import java.time.ZoneId
import java.time.ZoneOffset
import java.util.*

class AfkServiceTest {

    private val playerId = UUID.randomUUID()
    private val playerName = "Alex"

    @Test
    fun `toggle flips manual state and carries the reason`() {
        val service = AfkService({ AfkSettings() }, clock())

        val on = service.toggle(playerId, playerName, "lunch")
        assertTrue(on.afk)
        assertEquals("lunch", on.reason)
        assertTrue(service.isAfk(playerId))

        val off = service.toggle(playerId, playerName, null)
        assertFalse(off.afk)
        assertFalse(service.isAfk(playerId))
    }

    @Test
    fun `auto afk fires after the inactivity delay`() {
        val clock = clock()
        val settings = AfkSettings(
            autoAfterSeconds = 10,
            checkIntervalTicks = 1,
            movementThreshold = 0.1,
            rotationThresholdDegrees = 1.0,
        )
        val service = AfkService({ settings }, clock)

        assertNull(tick(service, clock, 0.0))
        clock.advanceSeconds(11)

        val change = tick(service, clock, 0.0)
        assertTrue(change?.afk == true)
        assertTrue(service.isAfk(playerId))
    }

    @Test
    fun `movement clears auto afk`() {
        val clock = clock()
        val settings = AfkSettings(
            autoAfterSeconds = 5,
            checkIntervalTicks = 1,
            movementThreshold = 0.1,
            rotationThresholdDegrees = 1.0,
        )
        val service = AfkService({ settings }, clock)

        tick(service, clock, 0.0)
        clock.advanceSeconds(6)
        assertTrue(tick(service, clock, 0.0)?.afk == true)

        val change = tick(service, clock, 5.0)
        assertFalse(change?.afk ?: true)
        assertFalse(service.isAfk(playerId))
    }

    @Test
    fun `manual clear on activity only clears when configured`() {
        val clock = clock()
        val settings = AfkSettings(
            checkIntervalTicks = 1,
            manualClearOnActivity = true,
        )
        val service = AfkService({ settings }, clock)

        tick(service, clock, 0.0)
        service.toggle(playerId, playerName, null)
        assertTrue(service.isAfk(playerId))

        val change = tick(service, clock, 5.0)
        assertFalse(change?.afk ?: true)
        assertFalse(service.isAfk(playerId))
    }

    @Test
    fun `disabled afk never changes state`() {
        val clock = clock()
        val service = AfkService({ AfkSettings(enabled = false) }, clock)

        assertNull(tick(service, clock, 5.0))
        assertFalse(service.isAfk(playerId))
    }

    @Test
    fun `kick fires after the threshold unless exempt`() {
        val clock = clock()
        val service = AfkService(
            { AfkSettings(kickAfterSeconds = 30) },
            clock,
        )
        service.toggle(playerId, playerName, null)

        assertEquals(0L, service.afkSeconds(playerId, clock.instant()))
        assertFalse(service.shouldKick(playerId, clock.instant(), exempt = false))

        clock.advanceSeconds(31)
        assertEquals(31L, service.afkSeconds(playerId, clock.instant()))
        assertTrue(service.shouldKick(playerId, clock.instant(), exempt = false))
        assertFalse(service.shouldKick(playerId, clock.instant(), exempt = true))
    }

    @Test
    fun `a zero kick threshold never kicks`() {
        val clock = clock()
        val service = AfkService({ AfkSettings(kickAfterSeconds = 0) }, clock)
        service.toggle(playerId, playerName, null)
        clock.advanceSeconds(10_000)

        assertFalse(service.shouldKick(playerId, clock.instant(), exempt = false))
    }

    @Test
    fun `activity cancellation respects each trigger setting`() {
        val clock = clock()
        val service = AfkService(
            {
                AfkSettings(
                    cancelOnInteract = true,
                    cancelOnChat = false,
                    cancelOnFish = false,
                )
            },
            clock,
        )
        service.toggle(playerId, playerName, null)

        assertNull(
            service.cancelForActivity(
                playerId,
                playerName,
                setOf(AfkCancelTrigger.CHAT)
            )
        )
        assertTrue(service.isAfk(playerId))

        val change = service.cancelForActivity(
            playerId,
            playerName,
            setOf(AfkCancelTrigger.INTERACT)
        )
        assertNotNull(change)
        assertFalse(change!!.afk)
        assertFalse(service.isAfk(playerId))
    }

    @Test
    fun `activity cancellation is a no-op when the player is active`() {
        val clock = clock()
        val service = AfkService({ AfkSettings(cancelOnInteract = true) }, clock)

        assertNull(
            service.cancelForActivity(
                playerId,
                playerName,
                setOf(AfkCancelTrigger.INTERACT)
            )
        )
    }

    @Test
    fun `cancelling afk restarts the auto inactivity timer`() {
        val clock = clock()
        val settings = AfkSettings(
            autoAfterSeconds = 10,
            checkIntervalTicks = 1,
            cancelOnInteract = true,
        )
        val service = AfkService({ settings }, clock)

        tick(service, clock, 0.0)
        clock.advanceSeconds(5)
        service.toggle(playerId, playerName, null)
        service.cancelForActivity(
            playerId,
            playerName,
            setOf(AfkCancelTrigger.INTERACT)
        )

        clock.advanceSeconds(6)
        tick(service, clock, 0.0)

        assertFalse(service.isAfk(playerId))
    }

    @Test
    fun `sleep exclusion follows the policy while afk`() {
        val clock = clock()
        val service = AfkService({ AfkSettings(sleepIgnoresAfk = true) }, clock)

        service.toggle(playerId, playerName, null)
        assertTrue(SleepExclusionRegistry.isExcluded(playerId))

        service.toggle(playerId, playerName, null)
        assertFalse(SleepExclusionRegistry.isExcluded(playerId))
    }

    private fun tick(
        service: AfkService,
        clock: MutableClock,
        x: Double
    ): AfkChange? =
        service.onTick(
            playerId = playerId,
            playerName = playerName,
            x = x,
            y = 64.0,
            z = 0.0,
            yaw = 0f,
            pitch = 0f,
        )

    private fun clock(): MutableClock =
        MutableClock(Instant.ofEpochSecond(10_000))

    private class MutableClock(
        private var current: Instant,
    ) : Clock() {

        fun advanceSeconds(seconds: Long) {
            current = current.plusSeconds(seconds)
        }

        override fun getZone(): ZoneId =
            ZoneOffset.UTC

        override fun withZone(zone: ZoneId): Clock =
            this

        override fun instant(): Instant =
            current

    }

}
