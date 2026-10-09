package top.likoslupus.cellulosesz.movement.command

import top.likoslupus.cellulosesz.core.command.CommandCategory
import top.likoslupus.cellulosesz.core.command.CommandDescriptor
import top.likoslupus.cellulosesz.core.permission.CommandPermissions
import top.likoslupus.cellulosesz.core.permission.PermissionSpec

/**
 * Every top-level command owned by the movement context. Declarative metadata only: the Brigadier
 * tree is still registered explicitly by `HomeCommands`, `WarpCommands`, `SpawnCommands`,
 * `TeleportCommands` and `TeleportRequestCommands`. `verifyArchitecture` compares this catalog
 * against the actual `dispatcher.register` calls.
 */
public object MovementCommandCatalog {

    public val descriptors: List<CommandDescriptor> = listOf(
        command("sethome", CommandPermissions.SET_HOME, playerOnly = true),
        command("home", CommandPermissions.HOME, playerOnly = true),
        command("delhome", CommandPermissions.DEL_HOME, playerOnly = true),
        command("homes", CommandPermissions.HOMES, playerOnly = true),
        command("back", CommandPermissions.BACK, playerOnly = true),
        command("tp", CommandPermissions.TP, playerOnly = false),
        command("tphere", CommandPermissions.TP_HERE, playerOnly = true),
        command("tppos", CommandPermissions.TP_POS, playerOnly = true),
        command("tpa", CommandPermissions.TPA, playerOnly = true),
        command("tpahere", CommandPermissions.TPA_HERE, playerOnly = true),
        command("tpaccept", CommandPermissions.TP_ACCEPT, playerOnly = true),
        command("tpdeny", CommandPermissions.TP_DENY, playerOnly = true),
        command("tpcancel", CommandPermissions.TP_CANCEL, playerOnly = true),
        command("warp", CommandPermissions.WARP, playerOnly = true),
        command("warps", CommandPermissions.WARPS, playerOnly = true),
        command("setwarp", CommandPermissions.SET_WARP, playerOnly = true),
        command("delwarp", CommandPermissions.DEL_WARP, playerOnly = true),
        command("spawn", CommandPermissions.SPAWN, playerOnly = true),
        command("setspawn", CommandPermissions.SET_SPAWN, playerOnly = true),
        command("delspawn", CommandPermissions.DEL_SPAWN, playerOnly = true),
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
        CommandCategory.MOVEMENT,
        permission,
        playerOnly,
        aliases
    )
