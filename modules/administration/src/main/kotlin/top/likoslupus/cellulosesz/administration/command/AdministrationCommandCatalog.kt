package top.likoslupus.cellulosesz.administration.command

import top.likoslupus.cellulosesz.core.command.CommandCategory
import top.likoslupus.cellulosesz.core.command.CommandDescriptor
import top.likoslupus.cellulosesz.core.permission.CommandPermissions
import top.likoslupus.cellulosesz.core.permission.PermissionSpec

/**
 * Every top-level command owned by the administration context (moderation, operator control, social
 * spy, vanish, player state). Declarative metadata only: the Brigadier tree is still registered
 * explicitly by the per-family command objects. `verifyArchitecture` compares this catalog against
 * the actual registrations.
 */
public object AdministrationCommandCatalog {

    public val descriptors: List<CommandDescriptor> = listOf(
        command("kick", CommandPermissions.KICK, playerOnly = false),
        command("kickall", CommandPermissions.KICK_ALL, playerOnly = false),
        command("ban", CommandPermissions.BAN, playerOnly = false),
        command("tempban", CommandPermissions.TEMP_BAN, playerOnly = false),
        command("unban", CommandPermissions.UNBAN, playerOnly = false),
        command("banip", CommandPermissions.BAN_IP, playerOnly = false),
        command("tempbanip", CommandPermissions.TEMP_BAN_IP, playerOnly = false),
        command("unbanip", CommandPermissions.UNBAN_IP, playerOnly = false),
        command("mute", CommandPermissions.MUTE, playerOnly = false),
        command("tempmute", CommandPermissions.TEMP_MUTE, playerOnly = false),
        command("unmute", CommandPermissions.UNMUTE, playerOnly = false),
        command("muteinfo", CommandPermissions.MUTE_INFO, playerOnly = false),
        command("kill", CommandPermissions.KILL, playerOnly = false),
        command("gamemode", CommandPermissions.GAMEMODE, playerOnly = false),
        command("sudo", CommandPermissions.SUDO, playerOnly = false),
        command("heal", CommandPermissions.HEAL, playerOnly = false),
        command("feed", CommandPermissions.FEED, playerOnly = false),
        command("fly", CommandPermissions.FLY, playerOnly = false),
        command("god", CommandPermissions.GOD, playerOnly = false),
        command("socialspy", CommandPermissions.SOCIAL_SPY, playerOnly = true),
        command("vanish", CommandPermissions.VANISH, playerOnly = true),
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
        CommandCategory.ADMINISTRATION,
        permission,
        playerOnly,
        aliases
    )
