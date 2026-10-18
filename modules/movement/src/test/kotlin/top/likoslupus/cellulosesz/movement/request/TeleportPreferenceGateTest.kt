package top.likoslupus.cellulosesz.movement.request

import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test
import top.likoslupus.cellulosesz.movement.config.TeleportRequestSettings
import java.util.*
import kotlin.time.TestTimeSource

/**
 * The `/tptoggle` decision: a target that disabled teleport requests is refused before any queue
 * check, unless the sender holds the bypass capability.
 */
class TeleportPreferenceGateTest {

    private val sender = UUID.randomUUID()
    private val target = UUID.randomUUID()
    private val other = UUID.randomUUID()

    private fun service(rejecting: Set<UUID>): TeleportRequestService =
        TeleportRequestService(
            settings = { TeleportRequestSettings() },
            targetAccepts = { it !in rejecting },
            timeSource = TestTimeSource(),
        )

    @Test
    fun `a target that accepts is allowed without a bypass`() {
        assertTrue(
            TeleportPreferenceGate.allowsTarget(
                targetAccepts = true,
                actorBypasses = false
            )
        )
    }

    @Test
    fun `a disabled target is refused without a bypass`() {
        assertFalse(
            TeleportPreferenceGate.allowsTarget(
                targetAccepts = false,
                actorBypasses = false
            )
        )
    }

    @Test
    fun `a bypass overrides a disabled target`() {
        assertTrue(
            TeleportPreferenceGate.allowsTarget(
                targetAccepts = false,
                actorBypasses = true
            )
        )
    }

    @Test
    fun `the service reports a disabled target and queues nothing`() {
        val service = service(setOf(target))

        assertEquals(
            TpaSendResult.TargetDisabled(target),
            service.send(
                sender,
                target,
                TeleportRequestType.TO_TARGET
            ),
        )
        assertInstanceOf(
            TpaAcceptResult.None::class.java,
            service.accept(target, sender)
        )
        assertTrue(service.cancel(sender).isEmpty())
    }

    @Test
    fun `a bypass queues the request for a disabled target`() {
        val service = service(setOf(target))

        assertEquals(
            TpaSendResult.Sent(target),
            service.send(
                sender,
                target,
                TeleportRequestType.TO_TARGET,
                bypassTargetChoice = true,
            ),
        )
        assertInstanceOf(
            TpaAcceptResult.Accepted::class.java,
            service.accept(target, sender)
        )
    }

    @Test
    fun `a batch skips disabled targets`() {
        val service = service(setOf(other))

        val result = assertInstanceOf(
            TpaBatchSendResult.Sent::class.java,
            service.sendBatch(
                sender,
                listOf(other, target),
                TeleportRequestType.BRING_TARGET,
            ) { true },
        )
        assertEquals(listOf(target), result.recipients)
    }

    @Test
    fun `a batch reports no recipients when every candidate is disabled`() {
        val service = service(setOf(other, target))

        assertEquals(
            TpaBatchSendResult.NoRecipients,
            service.sendBatch(
                sender,
                listOf(other, target),
                TeleportRequestType.BRING_TARGET,
            ) { true },
        )
    }

}
