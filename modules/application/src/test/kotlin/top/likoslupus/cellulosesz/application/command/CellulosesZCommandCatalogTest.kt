package top.likoslupus.cellulosesz.application.command

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import top.likoslupus.cellulosesz.core.command.CommandCatalog
import top.likoslupus.cellulosesz.core.permission.CommandPermissions

class CellulosesZCommandCatalogTest {

    @Test
    fun `the aggregated command catalog is well-formed`() {
        assertEquals(
            emptyList<String>(),
            CommandCatalog.validate(CellulosesZCommandCatalog.all),
        )
    }

    @Test
    fun `every command permission node has exactly one descriptor`() {
        val commandNodes = CommandPermissions.all
                .filter { it.node.startsWith(CommandPermissions.PREFIX) }
                .toSet()
        val described = CellulosesZCommandCatalog.all
                .map { it.permission }
                .toSet()

        assertEquals(commandNodes, described)
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
