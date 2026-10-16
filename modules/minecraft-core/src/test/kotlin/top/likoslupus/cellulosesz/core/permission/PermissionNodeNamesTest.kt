package top.likoslupus.cellulosesz.core.permission

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows

/**
 * The NeoForge `PermissionNode` adapter composes `modId + "." + localName`, so the split must keep
 * the node dot-separated. A regression here silently registered `cellulosesz.command/tp`, a node no
 * Fabric/LuckPerms grant could ever target.
 */
class PermissionNodeNamesTest {

    @Test
    fun `splits the mod namespace and keeps the remaining path dot-separated`() {
        assertEquals(
            "command.tp",
            PermissionNodeNames.localName("cellulosesz.command.tp")
        )
        assertEquals(
            "command.cellulosesz.reload",
            PermissionNodeNames.localName("cellulosesz.command.cellulosesz.reload"),
        )
        assertEquals(
            "moderation.moderator",
            PermissionNodeNames.localName("cellulosesz.moderation.moderator"),
        )
    }

    @Test
    fun `every declared node round-trips through the mod namespace`() {
        CommandPermissions.all.forEach { spec ->
            val recomposed = "${PermissionNodeNames.MOD_ID}.${PermissionNodeNames.localName(spec.node)}"
            assertEquals(spec.node, recomposed)
        }
    }

    @Test
    fun `rejects a node without the mod namespace`() {
        assertThrows<IllegalArgumentException> {
            PermissionNodeNames.localName("command.tp")
        }
    }

    @Test
    fun `rejects an empty local part`() {
        assertThrows<IllegalArgumentException> {
            PermissionNodeNames.localName("cellulosesz.")
        }
    }

    @Test
    fun `rejects a slash or colon separator in the local part`() {
        assertThrows<IllegalArgumentException> {
            PermissionNodeNames.localName("cellulosesz.command/tp")
        }
        assertThrows<IllegalArgumentException> {
            PermissionNodeNames.localName("cellulosesz.command:tp")
        }
        assertThrows<IllegalArgumentException> {
            PermissionNodeNames.localName("cellulosesz.command tp")
        }
    }

}
