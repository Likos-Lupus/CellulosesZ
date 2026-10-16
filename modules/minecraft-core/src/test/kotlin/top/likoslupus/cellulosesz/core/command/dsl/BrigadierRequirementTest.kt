package top.likoslupus.cellulosesz.core.command.dsl

import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

/**
 * Guards the Brigadier `requires` composition contract: a node's source access and its permission
 * node must be ANDed into one predicate.
 *
 * Mojang Brigadier's `requires` is a *setter*, so the previous two-call form silently dropped the
 * source gate: a command declared NON_PLAYER (such as `/sudo`) became reachable by a player whenever
 * their permission node was allowed. This test reproduces that regression without bootstrapping
 * Minecraft.
 */
class BrigadierRequirementTest {

    private enum class FakeSource {

        PLAYER,
        CONSOLE

    }

    @Test
    fun `access and permission must both allow`() {
        val requirement = combineRequirements<FakeSource>(
            accessAllowed = { it == FakeSource.CONSOLE },
            permissionAllowed = { true },
        )

        assertTrue(requirement.test(FakeSource.CONSOLE))
        assertFalse(requirement.test(FakeSource.PLAYER))
    }

    @Test
    fun `an allowed permission cannot override a failed source gate`() {
        val requirement = combineRequirements<FakeSource>(
            accessAllowed = { it == FakeSource.CONSOLE },
            permissionAllowed = { _ -> true },
        )

        assertFalse(requirement.test(FakeSource.PLAYER))
    }

    @Test
    fun `a passing source gate cannot override a denied permission`() {
        val requirement = combineRequirements<FakeSource>(
            accessAllowed = { true },
            permissionAllowed = { it == FakeSource.CONSOLE },
        )

        assertFalse(requirement.test(FakeSource.PLAYER))
        assertTrue(requirement.test(FakeSource.CONSOLE))
    }

    @Test
    fun `both gates denied`() {
        val requirement = combineRequirements<FakeSource>(
            accessAllowed = { false },
            permissionAllowed = { false },
        )

        assertFalse(requirement.test(FakeSource.PLAYER))
    }

}
