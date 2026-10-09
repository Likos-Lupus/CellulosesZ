package top.likoslupus.cellulosesz.utility.command

import top.likoslupus.cellulosesz.core.command.CommandCategory
import top.likoslupus.cellulosesz.core.command.CommandDescriptor
import top.likoslupus.cellulosesz.core.permission.CommandPermissions
import top.likoslupus.cellulosesz.core.permission.PermissionSpec

/**
 * Every top-level command owned by the utility context (kits, item utilities, workstations,
 * inspection). Declarative metadata only: the Brigadier tree is still registered explicitly by the
 * per-family command objects. `verifyArchitecture` compares this catalog against the registrations.
 */
public object UtilityCommandCatalog {

    public val descriptors: List<CommandDescriptor> = listOf(
        command("kit", CommandPermissions.KIT, playerOnly = true),
        command("kits", CommandPermissions.KITS, playerOnly = false),
        command("showkit", CommandPermissions.SHOW_KIT, playerOnly = false),
        command("createkit", CommandPermissions.CREATE_KIT, playerOnly = true),
        command("updatekit", CommandPermissions.UPDATE_KIT, playerOnly = true),
        command("delkit", CommandPermissions.DEL_KIT, playerOnly = false),
        command("kitreset", CommandPermissions.KIT_RESET, playerOnly = false),
        command("repair", CommandPermissions.REPAIR, playerOnly = false),
        command("more", CommandPermissions.MORE, playerOnly = true),
        command("condense", CommandPermissions.CONDENSE, playerOnly = true),
        command("enderchest", CommandPermissions.ENDER_CHEST, playerOnly = true),
        command("disposal", CommandPermissions.DISPOSAL, playerOnly = true),
        command("invsee", CommandPermissions.INVSEE, playerOnly = true),
        command("workbench", CommandPermissions.WORKBENCH, playerOnly = true),
        command("anvil", CommandPermissions.ANVIL, playerOnly = true),
        command("grindstone", CommandPermissions.GRINDSTONE, playerOnly = true),
        command("stonecutter", CommandPermissions.STONECUTTER, playerOnly = true),
        command("loom", CommandPermissions.LOOM, playerOnly = true),
        command("cartographytable", CommandPermissions.CARTOGRAPHY_TABLE, playerOnly = true),
        command("smithingtable", CommandPermissions.SMITHING_TABLE, playerOnly = true),
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
        CommandCategory.UTILITY,
        permission,
        playerOnly,
        aliases
    )
