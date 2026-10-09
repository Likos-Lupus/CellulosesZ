package top.likoslupus.cellulosesz.core.permission

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

class CommandPermissionsTest {

    @Test
    fun `every node is unique, lowercase and cellulosesz-prefixed`() {
        val nodes = CommandPermissions.all.map { it.node }

        assertEquals(
            nodes.size,
            nodes.toSet().size,
            "duplicate permission node in the catalog"
        )
        assertTrue(
            nodes.all { it == it.lowercase() },
            "permission nodes must be lowercase"
        )
        assertTrue(
            nodes.all { it.startsWith("cellulosesz.") },
            "permission nodes must be cellulosesz-prefixed"
        )
    }

    @Test
    fun `command nodes use the command prefix`() {
        assertTrue(CommandPermissions.BAN.node.startsWith(CommandPermissions.PREFIX))
        assertEquals(
            VanillaPermissionFallback.COMMANDS_MODERATOR,
            CommandPermissions.BAN.fallback
        )
        assertEquals(
            VanillaPermissionFallback.ALLOW_ALL,
            CommandPermissions.HOME.fallback
        )
    }

}
