package top.likoslupus.cellulosesz.application.command

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import top.likoslupus.cellulosesz.core.command.CommandCatalog
import top.likoslupus.cellulosesz.core.permission.CommandPermissions
import top.likoslupus.cellulosesz.core.permission.VanillaPermissionFallback

class CellulosesZCommandCatalogTest {

    @Test
    fun `the aggregated command catalog is well-formed`() {
        assertEquals(
            emptyList<String>(),
            CommandCatalog.validate(CellulosesZCommandCatalog.all),
        )
    }

    @Test
    fun `every command descriptor maps to a declared command permission`() {
        val commandNodes = CommandPermissions.all
                .filter { it.node.startsWith(CommandPermissions.PREFIX) }
                .toSet()
        val described = CellulosesZCommandCatalog.all
                .map { it.permission }
                .toSet()

        assertTrue(commandNodes.containsAll(described))
    }

    @Test
    fun `root subcommand capabilities are declared with the expected fallbacks`() {
        val nodes = CommandPermissions.all
                .filter { it.node.startsWith(CommandPermissions.PREFIX) }
                .toSet()

        assertTrue(nodes.contains(CommandPermissions.ROOT_LANGUAGE))
        assertTrue(nodes.contains(CommandPermissions.ROOT_STATUS))
        assertTrue(nodes.contains(CommandPermissions.ROOT_RELOAD))

        assertEquals(VanillaPermissionFallback.ALLOW_ALL, CommandPermissions.ROOT.fallback)
        assertEquals(VanillaPermissionFallback.ALLOW_ALL, CommandPermissions.ROOT_LANGUAGE.fallback)
        assertEquals(
            VanillaPermissionFallback.COMMANDS_MODERATOR,
            CommandPermissions.ROOT_STATUS.fallback
        )
        assertEquals(
            VanillaPermissionFallback.COMMANDS_MODERATOR,
            CommandPermissions.ROOT_RELOAD.fallback
        )
    }

    @Test
    fun `the moderator capability is not a command descriptor`() {
        assertTrue(
            CellulosesZCommandCatalog.all.none {
                it.permission == CommandPermissions.MODERATOR
            }
        )
    }

    @Test
    fun `the root command is part of the catalog`() {
        assertTrue(CellulosesZCommandCatalog.all.contains(CellulosesZCommandCatalog.root))
        assertEquals(
            CellulosesZCommandCatalog.byLiteral(RootCommand.ROOT_LITERAL),
            CellulosesZCommandCatalog.root
        )
    }

    @Test
    fun `aliases resolve to their primary descriptor`() {
        assertEquals(
            CommandPermissions.REPLY,
            CellulosesZCommandCatalog.byLiteral("r")?.permission
        )
    }

}
