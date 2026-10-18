package top.likoslupus.cellulosesz.core.permission

import top.likoslupus.cellulosesz.core.permission.PermissionNodeFactory.adminNode
import top.likoslupus.cellulosesz.core.permission.PermissionNodeFactory.moderatorNode
import top.likoslupus.cellulosesz.core.permission.PermissionNodeFactory.playerNode

/** Utility command nodes, grouped by the bounded context that owns the commands. */
internal object UtilityPermissionNodes {

    internal val KIT: PermissionSpec = playerNode(
        "kit",
        "Claim a kit"
    )

    internal val KITS: PermissionSpec = playerNode(
        "kits",
        "List kits"
    )

    internal val SHOW_KIT: PermissionSpec = playerNode(
        "showkit",
        "Show a kit"
    )

    internal val ENDER_CHEST: PermissionSpec = playerNode(
        "enderchest",
        "Open your ender chest"
    )

    internal val DISPOSAL: PermissionSpec = playerNode(
        "disposal",
        "Open a disposal"
    )

    internal val CONDENSE: PermissionSpec = playerNode(
        "condense",
        "Condense items"
    )

    internal val WORKBENCH: PermissionSpec = playerNode(
        "workbench",
        "Open a workbench"
    )

    internal val ANVIL: PermissionSpec = playerNode(
        "anvil",
        "Open an anvil"
    )

    internal val GRINDSTONE: PermissionSpec = playerNode(
        "grindstone",
        "Open a grindstone"
    )

    internal val STONECUTTER: PermissionSpec = playerNode(
        "stonecutter",
        "Open a stonecutter"
    )

    internal val LOOM: PermissionSpec = playerNode(
        "loom",
        "Open a loom"
    )

    internal val CARTOGRAPHY_TABLE: PermissionSpec = playerNode(
        "cartographytable",
        "Open a cartography table"
    )

    internal val SMITHING_TABLE: PermissionSpec = playerNode(
        "smithingtable",
        "Open a smithing table"
    )

    internal val GET_POS: PermissionSpec = playerNode(
        "getpos",
        "View your position"
    )

    internal val DEPTH: PermissionSpec = playerNode(
        "depth",
        "View your depth"
    )

    internal val HAT: PermissionSpec = playerNode(
        "hat",
        "Wear the held item as a hat"
    )

    internal val ITEM_NAME: PermissionSpec = playerNode(
        "itemname",
        "Rename the held item"
    )

    internal val ITEM_LORE: PermissionSpec = playerNode(
        "itemlore",
        "Edit the held item's lore"
    )

    internal val CLEAR_INVENTORY: PermissionSpec = playerNode(
        "clearinventory",
        "Clear your inventory"
    )

    internal val CLEAR_INVENTORY_OTHERS: PermissionSpec = moderatorNode(
        "clearinventory.others",
        "Clear another player's inventory"
    )

    internal val CLEAR_INVENTORY_CONFIRM_TOGGLE: PermissionSpec = playerNode(
        "clearinventoryconfirmtoggle",
        "Toggle the clear-inventory confirmation"
    )

    internal val REPAIR: PermissionSpec = moderatorNode(
        "repair",
        "Repair items"
    )

    internal val MORE: PermissionSpec = moderatorNode(
        "more",
        "Refill a stack"
    )

    internal val INVSEE: PermissionSpec = moderatorNode(
        "invsee",
        "Inspect an inventory"
    )

    internal val CREATE_KIT: PermissionSpec = moderatorNode(
        "createkit",
        "Create a kit"
    )

    internal val UPDATE_KIT: PermissionSpec = moderatorNode(
        "updatekit",
        "Update a kit"
    )

    internal val DEL_KIT: PermissionSpec = moderatorNode(
        "delkit",
        "Delete a kit"
    )

    internal val KIT_RESET: PermissionSpec = moderatorNode(
        "kitreset",
        "Reset kit claims"
    )

    internal val GIVE: PermissionSpec = moderatorNode(
        "give",
        "Give items to players"
    )

    internal val ITEM: PermissionSpec = moderatorNode(
        "item",
        "Give yourself an item"
    )

    internal val ITEM_DB: PermissionSpec = playerNode(
        "itemdb",
        "Look up item ids and aliases"
    )

    internal val ENCHANT: PermissionSpec = moderatorNode(
        "enchant",
        "Enchant the held item"
    )

    internal val ENCHANT_UNSAFE: PermissionSpec = PermissionSpec(
        "cellulosesz.enchant.unsafe",
        VanillaPermissionFallback.COMMANDS_ADMIN,
        "Use enchantment levels above the vanilla maximum"
    )

    internal val POTION: PermissionSpec = moderatorNode(
        "potion",
        "Edit the held potion"
    )

    internal val POTION_APPLY: PermissionSpec = PermissionSpec(
        "cellulosesz.potion.apply",
        VanillaPermissionFallback.COMMANDS_MODERATOR,
        "Apply the effects of the held potion to yourself"
    )

    internal val POTION_OTHERS: PermissionSpec = PermissionSpec(
        "cellulosesz.potion.others",
        VanillaPermissionFallback.COMMANDS_MODERATOR,
        "Edit another player's potion"
    )

    internal val SKULL: PermissionSpec = moderatorNode(
        "skull",
        "Create a player head"
    )

    internal val SKULL_OTHERS: PermissionSpec = PermissionSpec(
        "cellulosesz.skull.others",
        VanillaPermissionFallback.COMMANDS_MODERATOR,
        "Create a head for another owner"
    )

    internal val SKULL_SPAWN: PermissionSpec = PermissionSpec(
        "cellulosesz.skull.spawn",
        VanillaPermissionFallback.COMMANDS_MODERATOR,
        "Spawn a new head item instead of editing the held one"
    )

    internal val UNLIMITED: PermissionSpec = adminNode(
        "unlimited",
        "Toggle unlimited item use"
    )

    internal val UNLIMITED_OTHERS: PermissionSpec = PermissionSpec(
        "cellulosesz.unlimited.others",
        VanillaPermissionFallback.COMMANDS_ADMIN,
        "Toggle unlimited item use for another player"
    )

    internal val BOOK: PermissionSpec = playerNode(
        "book",
        "Edit the book in your hand"
    )

    internal val RECIPE: PermissionSpec = playerNode(
        "recipe",
        "Show an item's crafting recipe"
    )

    internal val POWERTOOL: PermissionSpec = playerNode(
        "powertool",
        "Bind a command to the held item"
    )

    internal val POWERTOOL_LIST: PermissionSpec = playerNode(
        "powertoollist",
        "List your powertools"
    )

    internal val POWERTOOL_TOGGLE: PermissionSpec = playerNode(
        "powertooltoggle",
        "Toggle your powertools on or off"
    )

    internal val ENDER_CHEST_OTHERS: PermissionSpec = PermissionSpec(
        "cellulosesz.enderchest.others",
        VanillaPermissionFallback.COMMANDS_MODERATOR,
        "Open another player's ender chest"
    )

    internal val INVSEE_EDIT: PermissionSpec = PermissionSpec(
        "cellulosesz.invsee.edit",
        VanillaPermissionFallback.COMMANDS_ADMIN,
        "Edit the inspected inventory"
    )

    internal val INVSEE_EQUIP: PermissionSpec = PermissionSpec(
        "cellulosesz.invsee.equip",
        VanillaPermissionFallback.COMMANDS_MODERATOR,
        "Inspect a player's equipment"
    )

}
