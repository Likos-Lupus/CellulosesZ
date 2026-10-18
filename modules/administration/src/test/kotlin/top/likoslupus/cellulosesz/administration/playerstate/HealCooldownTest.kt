package top.likoslupus.cellulosesz.administration.playerstate

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Test
import java.time.Duration
import java.time.Instant

class HealCooldownTest {

    private val now = Instant.ofEpochMilli(10_000)

    @Test
    fun `a fresh actor is never on cooldown`() {
        assertNull(
            HealCooldown.remaining(
                null,
                now,
                30,
                bypass = false
            )
        )
    }

    @Test
    fun `the remaining time shrinks as time passes`() {
        assertEquals(
            Duration.ofSeconds(20),
            HealCooldown.remaining(
                now.minusSeconds(10),
                now,
                30,
                bypass = false
            ),
        )
        assertNull(
            HealCooldown.remaining(
                now.minusSeconds(30),
                now,
                30,
                bypass = false
            )
        )
    }

    @Test
    fun `a bypass or a zero cooldown disables the limit`() {
        assertNull(
            HealCooldown.remaining(
                now,
                now,
                30,
                bypass = true
            )
        )
        assertNull(
            HealCooldown.remaining(
                now,
                now,
                0,
                bypass = false
            )
        )
    }

}
