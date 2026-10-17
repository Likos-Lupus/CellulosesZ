package top.likoslupus.cellulosesz.application.api

import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

/**
 * The commit fence guarantees the money-cancel contract: cancellation succeeds only before the commit
 * region begins, never once it has been claimed, and completing a request never overwrites a
 * cancellation.
 */
class MoneyCommitFenceTest {

    @Test
    fun `the commit region can only be claimed once`() {
        val fence = MoneyCommitFence()
        assertTrue(fence.tryBeginCommit())
        assertFalse(fence.tryBeginCommit())
    }

    @Test
    fun `cancellation is refused once the commit region is claimed`() {
        val fence = MoneyCommitFence()
        assertTrue(fence.tryBeginCommit())
        assertFalse(fence.cancelBeforeCommit())
        assertFalse(fence.isCancelled())
    }

    @Test
    fun `a cancellation before the commit region blocks the claim`() {
        val fence = MoneyCommitFence()
        assertTrue(fence.cancelBeforeCommit())
        assertFalse(fence.tryBeginCommit())
        assertTrue(fence.isCancelled())
    }

    @Test
    fun `cancellation is single-shot`() {
        val fence = MoneyCommitFence()
        assertTrue(fence.cancelBeforeCommit())
        assertFalse(fence.cancelBeforeCommit())
    }

    @Test
    fun `finishing never overwrites a cancellation`() {
        val fence = MoneyCommitFence()
        assertTrue(fence.cancelBeforeCommit())
        fence.finish()
        assertTrue(fence.isCancelled())
    }

    @Test
    fun `finishing a committed request leaves it uncancelled`() {
        val fence = MoneyCommitFence()
        assertTrue(fence.tryBeginCommit())
        fence.finish()
        assertFalse(fence.isCancelled())
        assertFalse(fence.cancelBeforeCommit())
    }

}
