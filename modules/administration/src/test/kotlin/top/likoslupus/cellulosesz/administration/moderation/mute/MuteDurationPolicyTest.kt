package top.likoslupus.cellulosesz.administration.moderation.mute

import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

class MuteDurationPolicyTest {

    @Test
    fun `permanent mutes are always permitted`() {
        assertTrue(
            MuteDurationPolicy.permits(
                null,
                capSeconds = 60,
                unlimited = false
            )
        )
    }

    @Test
    fun `the cap rejects anything above it`() {
        assertTrue(
            MuteDurationPolicy.permits(
                60,
                60,
                unlimited = false
            )
        )
        assertFalse(
            MuteDurationPolicy.permits(
                61,
                60,
                unlimited = false
            )
        )
    }

    @Test
    fun `the unlimited permission bypasses the cap`() {
        assertTrue(
            MuteDurationPolicy.permits(
                10_000,
                60,
                unlimited = true
            )
        )
    }

    @Test
    fun `no cap means no limit`() {
        assertTrue(
            MuteDurationPolicy.permits(
                10_000,
                null,
                unlimited = false
            )
        )
    }

}
