package top.likoslupus.cellulosesz.movement.teleport.cooldown

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Test
import java.util.*
import kotlin.time.Duration.Companion.seconds
import kotlin.time.TestTimeSource

class TeleportCooldownsTest {

    private val timeSource = TestTimeSource()
    private val cooldowns = TeleportCooldowns(timeSource)
    private val player = UUID.randomUUID()

    @Test
    fun `no cooldown before any success`() {
        assertNull(cooldowns.remaining(player, 10.seconds))
    }

    @Test
    fun `success starts a cooldown that expires`() {
        cooldowns.recordSuccess(player)

        assertEquals(
            10.seconds,
            cooldowns.remaining(player, 10.seconds)
        )

        timeSource += 4.seconds
        assertEquals(
            6.seconds,
            cooldowns.remaining(player, 10.seconds)
        )

        timeSource += 6.seconds
        assertNull(cooldowns.remaining(player, 10.seconds))
    }

    @Test
    fun `zero cooldown is always available`() {
        cooldowns.recordSuccess(player)

        assertNull(cooldowns.remaining(player, 0.seconds))
    }

}
