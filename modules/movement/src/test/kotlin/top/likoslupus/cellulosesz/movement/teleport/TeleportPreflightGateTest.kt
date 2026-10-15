package top.likoslupus.cellulosesz.movement.teleport

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Test
import top.likoslupus.cellulosesz.movement.TeleportOrigin
import top.likoslupus.cellulosesz.movement.TeleportPreflight
import top.likoslupus.cellulosesz.movement.TeleportPreflightDecision
import top.likoslupus.cellulosesz.movement.TeleportVetoReason
import java.util.*

class TeleportPreflightGateTest {

    private val subject = UUID.randomUUID()

    private val resolved = StoredPosition(
        dimension = "minecraft:overworld",
        x = 1.0,
        y = 64.0,
        z = 2.0,
        yaw = 0f,
        pitch = 0f,
    )

    @Test
    fun `the default preflight allows everything`() {
        assertNull(
            TeleportPreflightGate.evaluate(
                TeleportPreflight.AllowAll,
                subject,
                TeleportCause.HOME,
                resolved,
            )
        )
    }

    @Test
    fun `a deny returns its reason`() {
        val gate = TeleportPreflight {
            TeleportPreflightDecision.Deny(TeleportVetoReason.REGION_DENIED)
        }

        assertEquals(
            TeleportVetoReason.REGION_DENIED,
            TeleportPreflightGate.evaluate(
                gate,
                subject,
                TeleportCause.HOME,
                resolved
            ),
        )
    }

    @Test
    fun `a throwing preflight fails closed`() {
        val gate = TeleportPreflight { error("preflight boom") }

        assertEquals(
            TeleportVetoReason.PLATFORM_POLICY,
            TeleportPreflightGate.evaluate(
                gate,
                subject,
                TeleportCause.HOME,
                resolved
            ),
        )
    }

    @Test
    fun `the prepared request exposes a mapped origin and the resolved endpoint`() {
        var origin: TeleportOrigin? = null
        var endpoint: Double? = null
        val gate = TeleportPreflight {
            origin = it.origin
            endpoint = it.destination.y
            TeleportPreflightDecision.Allow
        }

        TeleportPreflightGate.evaluate(
            gate,
            subject,
            TeleportCause.JAIL_CONFINEMENT,
            resolved
        )

        assertEquals(TeleportOrigin.JAIL, origin)
        assertEquals(64.0, endpoint)
    }

}
