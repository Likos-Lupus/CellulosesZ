package top.likoslupus.cellulosesz.communication.command

import top.likoslupus.cellulosesz.core.command.CommandCategory
import top.likoslupus.cellulosesz.core.command.CommandDescriptor
import top.likoslupus.cellulosesz.core.permission.CommandPermissions
import top.likoslupus.cellulosesz.core.permission.PermissionSpec

/**
 * Every top-level command owned by the communication context. Declarative metadata only: the
 * Brigadier tree is still registered explicitly by the messaging/preferences/mail/staff/announcement
 * command objects. `verifyArchitecture` compares this catalog against the actual registrations.
 */
public object CommunicationCommandCatalog {

    public val descriptors: List<CommandDescriptor> = listOf(
        command("msg", CommandPermissions.MSG, playerOnly = true),
        command("reply", CommandPermissions.REPLY, playerOnly = true, aliases = setOf("r")),
        command("msgtoggle", CommandPermissions.MSG_TOGGLE, playerOnly = true),
        command("ignore", CommandPermissions.IGNORE, playerOnly = true),
        command("mail", CommandPermissions.MAIL, playerOnly = false),
        command("helpop", CommandPermissions.HELP_OP, playerOnly = false),
        command("broadcast", CommandPermissions.BROADCAST, playerOnly = false),
        command("broadcastworld", CommandPermissions.BROADCAST_WORLD, playerOnly = false),
    )

}

private fun command(
    literal: String,
    permission: PermissionSpec,
    playerOnly: Boolean,
    aliases: Set<String> = emptySet(),
): CommandDescriptor =
    CommandDescriptor(
        literal,
        CommandCategory.COMMUNICATION,
        permission,
        playerOnly,
        aliases
    )
