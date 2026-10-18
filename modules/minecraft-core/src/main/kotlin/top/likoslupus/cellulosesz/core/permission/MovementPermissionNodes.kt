package top.likoslupus.cellulosesz.core.permission

import top.likoslupus.cellulosesz.core.permission.PermissionNodeFactory.adminNode
import top.likoslupus.cellulosesz.core.permission.PermissionNodeFactory.moderatorNode
import top.likoslupus.cellulosesz.core.permission.PermissionNodeFactory.playerNode

/** Movement command nodes, grouped by the bounded context that owns the commands. */
internal object MovementPermissionNodes {

    internal val SET_HOME: PermissionSpec = playerNode(
        "sethome",
        "Set a home"
    )

    internal val HOME: PermissionSpec = playerNode(
        "home",
        "Teleport to a home"
    )

    internal val DEL_HOME: PermissionSpec = playerNode(
        "delhome",
        "Delete a home"
    )

    internal val HOMES: PermissionSpec = playerNode(
        "homes",
        "List homes"
    )

    internal val RENAME_HOME: PermissionSpec = playerNode(
        "renamehome",
        "Rename a home"
    )

    internal val WARP: PermissionSpec = playerNode(
        "warp",
        "Use a warp"
    )

    internal val WARPS: PermissionSpec = playerNode(
        "warps",
        "List warps"
    )

    internal val SET_WARP: PermissionSpec = moderatorNode(
        "setwarp",
        "Create a warp"
    )

    internal val DEL_WARP: PermissionSpec = moderatorNode(
        "delwarp",
        "Delete a warp"
    )

    internal val WARP_INFO: PermissionSpec = playerNode(
        "warpinfo",
        "View warp details"
    )

    internal val WARP_INFO_COORDINATES: PermissionSpec = moderatorNode(
        "warpinfo.coordinates",
        "View exact warp coordinates"
    )

    internal val SPAWN: PermissionSpec = playerNode(
        "spawn",
        "Teleport to spawn"
    )

    internal val SET_SPAWN: PermissionSpec = moderatorNode(
        "setspawn",
        "Set spawn"
    )

    internal val DEL_SPAWN: PermissionSpec = moderatorNode(
        "delspawn",
        "Reset spawn"
    )

    internal val BACK: PermissionSpec = playerNode(
        "back",
        "Return to the previous location"
    )

    internal val TP: PermissionSpec = playerNode(
        "tp",
        "Teleport"
    )

    /**
     * The extra gate for using the CellulosesZ implementation through the *vanilla* `/tp` entry
     * when takeover is enabled. The ordinary `/cztp` and `/cellulosesz:tp` entries keep using [TP]
     * only, so taking over the vanilla literal never widens an ordinary player's access.
     */
    internal val TP_VANILLA_OVERRIDE: PermissionSpec = PermissionSpec(
        "cellulosesz.command.override.tp",
        VanillaPermissionFallback.COMMANDS_GAMEMASTER,
        "Use the CellulosesZ implementation through the vanilla /tp entry",
    )

    internal val TP_HERE: PermissionSpec = moderatorNode(
        "tphere",
        "Teleport a player to you"
    )

    internal val TP_POS: PermissionSpec = moderatorNode(
        "tppos",
        "Teleport to coordinates"
    )

    internal val TPR: PermissionSpec = playerNode(
        "tpr",
        "Teleport to a random safe location"
    )

    internal val SET_TPR: PermissionSpec = moderatorNode(
        "settpr",
        "Define a random teleport region"
    )

    internal val TOP: PermissionSpec = playerNode(
        "top",
        "Teleport to the surface"
    )

    internal val BOTTOM: PermissionSpec = playerNode(
        "bottom",
        "Teleport to the bottom of the world"
    )

    internal val JUMP: PermissionSpec = playerNode(
        "jump",
        "Teleport to the block you are looking at"
    )

    internal val TPA: PermissionSpec = playerNode(
        "tpa",
        "Request a teleport"
    )

    internal val TPA_HERE: PermissionSpec = playerNode(
        "tpahere",
        "Request a teleport here"
    )

    internal val TP_ACCEPT: PermissionSpec = playerNode(
        "tpaccept",
        "Accept a teleport request"
    )

    internal val TP_DENY: PermissionSpec = playerNode(
        "tpdeny",
        "Deny a teleport request"
    )

    internal val TP_CANCEL: PermissionSpec = playerNode(
        "tpcancel",
        "Cancel a teleport request"
    )

    internal val TPA_ALL: PermissionSpec = playerNode(
        "tpaall",
        "Request a teleport for every online player"
    )

    internal val TPAUTO: PermissionSpec = playerNode(
        "tpauto",
        "Manage automatic teleport-request acceptance"
    )

    internal val TP_TOGGLE: PermissionSpec = playerNode(
        "tptoggle",
        "Toggle accepting teleport requests"
    )

    /** Toggling another player's teleport setting; a non-command capability. */
    internal val TP_TOGGLE_OTHERS: PermissionSpec = PermissionSpec(
        "cellulosesz.tptoggle.others",
        VanillaPermissionFallback.COMMANDS_MODERATOR,
        "Toggle another player's teleport setting"
    )

    /** Teleporting to or moving a player who disabled teleports; a non-command capability. */
    internal val TP_TOGGLE_BYPASS: PermissionSpec = PermissionSpec(
        "cellulosesz.tptoggle.bypass",
        VanillaPermissionFallback.COMMANDS_MODERATOR,
        "Teleport regardless of a player's teleport setting"
    )

    internal val WORLD: PermissionSpec = playerNode(
        "world",
        "Switch worlds"
    )

    internal val TP_O: PermissionSpec = moderatorNode(
        "tpo",
        "Teleport directly to a player"
    )

    internal val TP_O_HERE: PermissionSpec = moderatorNode(
        "tpohere",
        "Teleport a player to you directly"
    )

    internal val TP_OFFLINE: PermissionSpec = moderatorNode(
        "tpoffline",
        "Teleport to an offline player's last known location"
    )

    internal val TP_ALL: PermissionSpec = adminNode(
        "tpall",
        "Teleport every player to a target"
    )

    internal val COMPASS: PermissionSpec = playerNode(
        "compass",
        "Show your current bearing"
    )

}
