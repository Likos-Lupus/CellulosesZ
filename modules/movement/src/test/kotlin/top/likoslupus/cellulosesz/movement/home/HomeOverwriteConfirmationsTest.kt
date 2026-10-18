package top.likoslupus.cellulosesz.movement.home

import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import java.time.Instant
import java.util.*
import kotlin.time.Duration.Companion.seconds

class HomeOverwriteConfirmationsTest {

    private val player = UUID.randomUUID()
    private var now: Instant = Instant.parse("2026-01-01T00:00:00Z")

    private val confirmations = HomeOverwriteConfirmations(30.seconds) { now }

    @Test
    fun `a remembered overwrite is confirmed exactly once`() {
        confirmations.remember(player, "base")
        assertTrue(confirmations.confirm(player, "base"))
        assertFalse(confirmations.confirm(player, "base"))
    }

    @Test
    fun `a different name is not confirmed`() {
        confirmations.remember(player, "base")
        assertFalse(confirmations.confirm(player, "other"))
        assertFalse(confirmations.confirm(player, "base"))
    }

    @Test
    fun `an expired confirmation is rejected`() {
        confirmations.remember(player, "base")
        now = now.plusSeconds(31)
        assertFalse(confirmations.confirm(player, "base"))
    }

    @Test
    fun `an unmatched player has nothing to confirm`() {
        confirmations.remember(player, "base")
        assertFalse(confirmations.confirm(UUID.randomUUID(), "base"))
    }

    @Test
    fun `clear drops the pending entry`() {
        confirmations.remember(player, "base")
        confirmations.clear(player)
        assertFalse(confirmations.confirm(player, "base"))
    }

}
