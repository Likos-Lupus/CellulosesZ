package top.likoslupus.cellulosesz.utility.item

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import java.time.Instant
import java.util.*

class ClearInventoryConfirmationsTest {

    private var now: Instant = Instant.parse("2026-01-01T00:00:00Z")
    private val actor = UUID.randomUUID()
    private val target = UUID.randomUUID()
    private val snapshot = InventorySnapshot(
        scope = ClearScope.MAIN,
        stacks = 1,
        fingerprints = listOf(SlotFingerprint("main:0", "digest")),
    )

    private fun confirmations(seconds: Int = 15): ClearInventoryConfirmations =
        ClearInventoryConfirmations(
            windowSeconds = { seconds },
            clock = { now },
            codeGenerator = { "123456" },
        )

    @Test
    fun `an issued code claims within the window`() {
        val confirmations = confirmations()

        assertEquals(
            "123456",
            confirmations.issue(actor, target, snapshot)
        )
        val claim = confirmations.claim(actor, "123456")
        assertEquals(
            ClearConfirmation(actor, target, snapshot),
            (claim as ClearClaim.Claimed).confirmation,
        )
    }

    @Test
    fun `a code can be used only once`() {
        val confirmations = confirmations()
        confirmations.issue(actor, target, snapshot)

        assertTrue(confirmations.claim(actor, "123456") is ClearClaim.Claimed)
        assertEquals(
            ClearClaim.Invalid,
            confirmations.claim(actor, "123456")
        )
    }

    @Test
    fun `an expired code is rejected`() {
        val confirmations = confirmations(seconds = 15)
        confirmations.issue(actor, target, snapshot)

        now = now.plusSeconds(16)

        assertEquals(
            ClearClaim.Invalid,
            confirmations.claim(actor, "123456")
        )
    }

    @Test
    fun `a wrong code is rejected without consuming the pending code`() {
        val confirmations = confirmations()
        confirmations.issue(actor, target, snapshot)

        assertEquals(
            ClearClaim.Invalid,
            confirmations.claim(actor, "000000")
        )
        assertTrue(confirmations.claim(actor, "123456") is ClearClaim.Claimed)
    }

    @Test
    fun `forget drops a pending code`() {
        val confirmations = confirmations()
        confirmations.issue(actor, target, snapshot)

        confirmations.forget(actor)

        assertEquals(
            ClearClaim.Invalid,
            confirmations.claim(actor, "123456")
        )
    }

}
