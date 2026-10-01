package top.likoslupus.cellulosesz.core.command

import net.minecraft.commands.CommandSourceStack
import net.minecraft.server.permissions.Permissions

/**
 * Single mapping point for the vanilla moderator capability. Command files must not repeat the
 * permission constant directly, so a future permission port only has to replace this function.
 */
public fun CommandSourceStack.canUseModeratorCommands(): Boolean =
    permissions().hasPermission(Permissions.COMMANDS_MODERATOR)
