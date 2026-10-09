package top.likoslupus.cellulosesz.application.command

import top.likoslupus.cellulosesz.administration.command.AdministrationCommandCatalog
import top.likoslupus.cellulosesz.communication.command.CommunicationCommandCatalog
import top.likoslupus.cellulosesz.core.command.CommandCategory
import top.likoslupus.cellulosesz.core.command.CommandDescriptor
import top.likoslupus.cellulosesz.core.permission.CommandPermissions
import top.likoslupus.cellulosesz.movement.command.MovementCommandCatalog
import top.likoslupus.cellulosesz.utility.command.UtilityCommandCatalog

/**
 * The complete CellulosesZ top-level command catalog: every bounded context's catalog plus the
 * `/cellulosesz` root. Used for validation tests and as the single source of truth for the command
 * table; it registers nothing.
 */
internal object CellulosesZCommandCatalog {

    internal val root: CommandDescriptor = CommandDescriptor(
        literal = RootCommand.ROOT_LITERAL,
        category = CommandCategory.ROOT,
        permission = CommandPermissions.ROOT,
        playerOnly = false,
    )

    internal val all: List<CommandDescriptor> =
        MovementCommandCatalog.descriptors +
                CommunicationCommandCatalog.descriptors +
                AdministrationCommandCatalog.descriptors +
                UtilityCommandCatalog.descriptors +
                root

    internal fun byLiteral(literal: String): CommandDescriptor? =
        all.firstOrNull { it.literal == literal || literal in it.aliases }

}
