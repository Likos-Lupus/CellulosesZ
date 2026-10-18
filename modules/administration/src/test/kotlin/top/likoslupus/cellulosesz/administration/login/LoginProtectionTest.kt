package top.likoslupus.cellulosesz.administration.login

import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import java.time.Instant
import java.util.*

class LoginProtectionTest {

    private val player = UUID.randomUUID()
    private val join = Instant.ofEpochMilli(1_000)

    @Test
    fun `a fresh join is protected for the whole window`() {
        val protection = LoginProtection()
        protection.recordJoin(player, join)
        assertTrue(
            protection.isProtected(
                player,
                join,
                5
            )
        )
        assertTrue(
            protection.isProtected(
                player,
                join.plusSeconds(4),
                5
            )
        )
        assertFalse(
            protection.isProtected(
                player,
                join.plusSeconds(5),
                5
            )
        )
    }

    @Test
    fun `an unknown player and a disabled delay are never protected`() {
        val protection = LoginProtection()
        assertFalse(
            protection.isProtected(
                player,
                join,
                5
            )
        )
        protection.recordJoin(player, join)
        assertFalse(
            protection.isProtected(
                player,
                join,
                0
            )
        )
    }

    @Test
    fun `forget removes the window`() {
        val protection = LoginProtection()
        protection.recordJoin(player, join)
        protection.forget(player)
        assertFalse(
            protection.isProtected(
                player,
                join,
                5
            )
        )
    }

}
