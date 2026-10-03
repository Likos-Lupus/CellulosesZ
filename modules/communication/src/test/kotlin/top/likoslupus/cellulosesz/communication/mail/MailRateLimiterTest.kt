package top.likoslupus.cellulosesz.communication.mail

import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import top.likoslupus.cellulosesz.communication.FakeTimeSource
import java.util.*
import kotlin.time.Duration.Companion.minutes
import kotlin.time.Duration.Companion.seconds

class MailRateLimiterTest {

    private val clock = FakeTimeSource()
    private val limiter = MailRateLimiter(clock)
    private val alice = UUID.randomUUID()
    private val bob = UUID.randomUUID()

    @Test
    fun `allows up to the limit within a window`() {
        assertTrue(limiter.tryAcquire(alice, 3))
        assertTrue(limiter.tryAcquire(alice, 3))
        assertTrue(limiter.tryAcquire(alice, 3))
        assertFalse(limiter.tryAcquire(alice, 3))
    }

    @Test
    fun `resets after the window elapses`() {
        assertTrue(limiter.tryAcquire(alice, 1))
        assertFalse(limiter.tryAcquire(alice, 1))

        clock.advance(61.seconds)

        assertTrue(limiter.tryAcquire(alice, 1))
    }

    @Test
    fun `senders are independent`() {
        assertTrue(limiter.tryAcquire(alice, 1))
        assertFalse(limiter.tryAcquire(alice, 1))
        assertTrue(limiter.tryAcquire(bob, 1))
    }

    @Test
    fun `non positive limit rejects`() {
        assertFalse(limiter.tryAcquire(alice, 0))
    }

    @Test
    fun `window boundary is inclusive of one minute`() {
        assertTrue(limiter.tryAcquire(alice, 1))
        clock.advance(1.minutes)
        assertTrue(limiter.tryAcquire(alice, 1))
    }

}
