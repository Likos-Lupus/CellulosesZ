package top.likoslupus.cellulosesz.api.command

import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test
import top.likoslupus.cellulosesz.api.command.execution.CommandDescriptor
import top.likoslupus.cellulosesz.api.command.execution.CommandOutcome

class CommandContractsTest {

    @Test
    fun commandOutcome_outcomes() {
        val success = CommandOutcome.success()
        assertTrue(success.successful())
        assertEquals(1, success.brigadierResult)
        assertEquals(CommandOutcome.Status.SUCCESS, success.status)

        val rejected = CommandOutcome.rejected()
        assertFalse(rejected.successful())
        assertEquals(0, rejected.brigadierResult)

        val partial = CommandOutcome.partial(2)
        assertFalse(partial.successful())
        assertEquals(CommandOutcome.Status.PARTIAL, partial.status)
        assertEquals(2, partial.brigadierResult)
    }

    @Test
    fun commandDescriptor_creation() {
        val descriptor = CommandDescriptor(
            "teleport",
            "tp",
            "cellulosesz.teleport.tp",
            CommandSourceKind.PLAYER_ONLY
        )
        assertEquals("teleport", descriptor.moduleId)
        assertEquals("tp", descriptor.canonicalName)
        assertEquals(CommandSourceKind.PLAYER_ONLY, descriptor.requiredSourceKind)
    }

}
