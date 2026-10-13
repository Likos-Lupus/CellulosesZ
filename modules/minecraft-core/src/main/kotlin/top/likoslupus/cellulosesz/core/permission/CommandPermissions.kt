package top.likoslupus.cellulosesz.core.permission

/**
 * The stable CellulosesZ permission node catalog. Nodes use `cellulosesz.command.<primary>`; a
 * player command still gets a node (fallback ALLOW_ALL) so an admin can explicitly deny it later.
 * Undefined nodes fall back to the exact vanilla behaviour.
 */
public object CommandPermissions {

    public const val PREFIX: String = "cellulosesz.command."

    private fun playerNode(name: String, description: String): PermissionSpec =
        PermissionSpec(
            PREFIX + name,
            VanillaPermissionFallback.ALLOW_ALL,
            description
        )

    private fun moderatorNode(name: String, description: String): PermissionSpec =
        PermissionSpec(
            PREFIX + name,
            VanillaPermissionFallback.COMMANDS_MODERATOR,
            description
        )

    public val ROOT: PermissionSpec =
        playerNode(
            "cellulosesz",
            "Use the CellulosesZ command"
        )

    public val ROOT_LANGUAGE: PermissionSpec =
        PermissionSpec(
            "cellulosesz.command.cellulosesz.language",
            VanillaPermissionFallback.ALLOW_ALL,
            "Change your CellulosesZ language"
        )

    public val ROOT_STATUS: PermissionSpec =
        PermissionSpec(
            "cellulosesz.command.cellulosesz.status",
            VanillaPermissionFallback.COMMANDS_MODERATOR,
            "View CellulosesZ status"
        )

    public val ROOT_RELOAD: PermissionSpec =
        PermissionSpec(
            "cellulosesz.command.cellulosesz.reload",
            VanillaPermissionFallback.COMMANDS_MODERATOR,
            "Reload CellulosesZ configuration"
        )

    /** Internal capability used by moderator notices and staff broadcasts (not a command node). */
    public val MODERATOR: PermissionSpec =
        PermissionSpec(
            "cellulosesz.moderation.moderator",
            VanillaPermissionFallback.COMMANDS_MODERATOR,
            "CellulosesZ moderator capability"
        )

    public val SET_HOME: PermissionSpec = playerNode(
        "sethome",
        "Set a home"
    )
    public val HOME: PermissionSpec = playerNode(
        "home",
        "Teleport to a home"
    )
    public val DEL_HOME: PermissionSpec = playerNode(
        "delhome",
        "Delete a home"
    )
    public val HOMES: PermissionSpec = playerNode(
        "homes",
        "List homes"
    )
    public val RENAME_HOME: PermissionSpec = playerNode(
        "renamehome",
        "Rename a home"
    )
    public val WARP: PermissionSpec = playerNode(
        "warp",
        "Use a warp"
    )
    public val WARPS: PermissionSpec = playerNode(
        "warps",
        "List warps"
    )
    public val SET_WARP: PermissionSpec = moderatorNode(
        "setwarp",
        "Create a warp"
    )
    public val DEL_WARP: PermissionSpec = moderatorNode(
        "delwarp",
        "Delete a warp"
    )
    public val WARP_INFO: PermissionSpec = playerNode(
        "warpinfo",
        "View warp details"
    )
    public val WARP_INFO_COORDINATES: PermissionSpec = moderatorNode(
        "warpinfo.coordinates",
        "View exact warp coordinates"
    )
    public val SPAWN: PermissionSpec = playerNode(
        "spawn",
        "Teleport to spawn"
    )
    public val SET_SPAWN: PermissionSpec = moderatorNode(
        "setspawn",
        "Set spawn"
    )
    public val DEL_SPAWN: PermissionSpec = moderatorNode(
        "delspawn",
        "Reset spawn"
    )
    public val BACK: PermissionSpec = playerNode(
        "back",
        "Return to the previous location"
    )
    public val TP: PermissionSpec = playerNode(
        "tp",
        "Teleport"
    )
    public val TP_HERE: PermissionSpec = moderatorNode(
        "tphere",
        "Teleport a player to you"
    )
    public val TP_POS: PermissionSpec = moderatorNode(
        "tppos",
        "Teleport to coordinates"
    )
    public val TPR: PermissionSpec = playerNode(
        "tpr",
        "Teleport to a random safe location"
    )
    public val SET_TPR: PermissionSpec = moderatorNode(
        "settpr",
        "Define a random teleport region"
    )
    public val TOP: PermissionSpec = playerNode(
        "top",
        "Teleport to the surface"
    )
    public val BOTTOM: PermissionSpec = playerNode(
        "bottom",
        "Teleport to the bottom of the world"
    )
    public val JUMP: PermissionSpec = playerNode(
        "jump",
        "Teleport to the block you are looking at"
    )
    public val TPA: PermissionSpec = playerNode(
        "tpa",
        "Request a teleport"
    )
    public val TPA_HERE: PermissionSpec = playerNode(
        "tpahere",
        "Request a teleport here"
    )
    public val TP_ACCEPT: PermissionSpec = playerNode(
        "tpaccept",
        "Accept a teleport request"
    )
    public val TP_DENY: PermissionSpec = playerNode(
        "tpdeny",
        "Deny a teleport request"
    )
    public val TP_CANCEL: PermissionSpec = playerNode(
        "tpcancel",
        "Cancel a teleport request"
    )
    public val TPA_ALL: PermissionSpec = playerNode(
        "tpaall",
        "Request a teleport for every online player"
    )
    public val TPAUTO: PermissionSpec = playerNode(
        "tpauto",
        "Manage automatic teleport-request acceptance"
    )
    public val MSG: PermissionSpec = playerNode(
        "msg",
        "Send a private message"
    )
    public val REPLY: PermissionSpec = playerNode(
        "reply",
        "Reply to a private message"
    )
    public val IGNORE: PermissionSpec = playerNode(
        "ignore",
        "Manage ignored players"
    )
    public val MSG_TOGGLE: PermissionSpec = playerNode(
        "msgtoggle",
        "Toggle private messages"
    )
    public val MAIL: PermissionSpec = playerNode(
        "mail",
        "Use mail"
    )
    public val HELP_OP: PermissionSpec = playerNode(
        "helpop",
        "Request staff help"
    )
    public val KIT: PermissionSpec = playerNode(
        "kit",
        "Claim a kit"
    )
    public val KITS: PermissionSpec = playerNode(
        "kits",
        "List kits"
    )
    public val SHOW_KIT: PermissionSpec = playerNode(
        "showkit",
        "Show a kit"
    )
    public val ENDER_CHEST: PermissionSpec = playerNode(
        "enderchest",
        "Open your ender chest"
    )
    public val DISPOSAL: PermissionSpec = playerNode(
        "disposal",
        "Open a disposal"
    )
    public val CONDENSE: PermissionSpec = playerNode(
        "condense",
        "Condense items"
    )
    public val WORKBENCH: PermissionSpec = playerNode(
        "workbench",
        "Open a workbench"
    )
    public val ANVIL: PermissionSpec = playerNode(
        "anvil",
        "Open an anvil"
    )
    public val GRINDSTONE: PermissionSpec = playerNode(
        "grindstone",
        "Open a grindstone"
    )
    public val STONECUTTER: PermissionSpec = playerNode(
        "stonecutter",
        "Open a stonecutter"
    )
    public val LOOM: PermissionSpec = playerNode(
        "loom",
        "Open a loom"
    )
    public val CARTOGRAPHY_TABLE: PermissionSpec = playerNode(
        "cartographytable",
        "Open a cartography table"
    )
    public val SMITHING_TABLE: PermissionSpec = playerNode(
        "smithingtable",
        "Open a smithing table"
    )
    public val GET_POS: PermissionSpec = playerNode(
        "getpos",
        "View your position"
    )
    public val DEPTH: PermissionSpec = playerNode(
        "depth",
        "View your depth"
    )
    public val HAT: PermissionSpec = playerNode(
        "hat",
        "Wear the held item as a hat"
    )
    public val ITEM_NAME: PermissionSpec = playerNode(
        "itemname",
        "Rename the held item"
    )
    public val ITEM_LORE: PermissionSpec = playerNode(
        "itemlore",
        "Edit the held item's lore"
    )
    public val CLEAR_INVENTORY: PermissionSpec = playerNode(
        "clearinventory",
        "Clear your inventory"
    )
    public val CLEAR_INVENTORY_OTHERS: PermissionSpec = moderatorNode(
        "clearinventory.others",
        "Clear another player's inventory"
    )

    public val SEEN: PermissionSpec = playerNode(
        "seen",
        "View a player's last seen time"
    )
    public val PLAY_TIME: PermissionSpec = playerNode(
        "playtime",
        "View your play time"
    )
    public val WHOIS: PermissionSpec = playerNode(
        "whois",
        "View player information"
    )
    public val AFK: PermissionSpec = playerNode(
        "afk",
        "Toggle AFK"
    )
    public val PING: PermissionSpec = playerNode(
        "ping",
        "View your latency"
    )
    public val PING_OTHERS: PermissionSpec = moderatorNode(
        "ping.others",
        "View another player's latency"
    )
    public val NEAR: PermissionSpec = playerNode(
        "near",
        "List nearby players"
    )
    public val NEAR_BYPASS_RADIUS: PermissionSpec = moderatorNode(
        "near.bypass_radius",
        "Exceed the configured /near radius"
    )
    public val NICK: PermissionSpec = playerNode(
        "nick",
        "Set or clear your nickname"
    )
    public val REALNAME: PermissionSpec = playerNode(
        "realname",
        "Resolve a nickname to a player name"
    )
    public val NICK_OTHERS: PermissionSpec = moderatorNode(
        "nick.others",
        "Set or clear another player's nickname"
    )
    public val PLAY_TIME_OTHERS: PermissionSpec = moderatorNode(
        "playtime.others",
        "View another player's play time"
    )

    public val HEAL: PermissionSpec = moderatorNode(
        "heal",
        "Heal a player"
    )
    public val FEED: PermissionSpec = moderatorNode(
        "feed",
        "Feed a player"
    )
    public val FLY: PermissionSpec = moderatorNode(
        "fly",
        "Toggle flight"
    )
    public val GOD: PermissionSpec = moderatorNode(
        "god",
        "Toggle invulnerability"
    )
    public val KICK: PermissionSpec = moderatorNode(
        "kick",
        "Kick a player"
    )
    public val KICK_ALL: PermissionSpec = moderatorNode(
        "kickall",
        "Kick every player"
    )
    public val BAN: PermissionSpec = moderatorNode(
        "ban",
        "Ban a player"
    )
    public val TEMP_BAN: PermissionSpec = moderatorNode(
        "tempban",
        "Temporarily ban a player"
    )
    public val UNBAN: PermissionSpec = moderatorNode(
        "unban",
        "Unban a player"
    )
    public val BAN_IP: PermissionSpec = moderatorNode(
        "banip",
        "Ban an IP"
    )
    public val TEMP_BAN_IP: PermissionSpec = moderatorNode(
        "tempbanip",
        "Temporarily ban an IP"
    )
    public val UNBAN_IP: PermissionSpec = moderatorNode(
        "unbanip",
        "Unban an IP"
    )
    public val MUTE: PermissionSpec = moderatorNode(
        "mute",
        "Mute a player"
    )
    public val TEMP_MUTE: PermissionSpec = moderatorNode(
        "tempmute",
        "Temporarily mute a player"
    )
    public val UNMUTE: PermissionSpec = moderatorNode(
        "unmute",
        "Unmute a player"
    )
    public val MUTE_INFO: PermissionSpec = moderatorNode(
        "muteinfo",
        "Inspect a mute"
    )
    public val SET_JAIL: PermissionSpec = moderatorNode(
        "setjail",
        "Create a jail"
    )
    public val JAIL: PermissionSpec = moderatorNode(
        "jail",
        "Jail a player"
    )
    public val UNJAIL: PermissionSpec = moderatorNode(
        "unjail",
        "Release a player from jail"
    )
    public val TOGGLE_JAIL: PermissionSpec = moderatorNode(
        "togglejail",
        "Toggle a player's jail state"
    )
    public val DEL_JAIL: PermissionSpec = moderatorNode(
        "deljail",
        "Delete a jail"
    )
    public val JAILS: PermissionSpec = moderatorNode(
        "jails",
        "List jails"
    )
    public val JAILED_PLAYERS: PermissionSpec = moderatorNode(
        "jailedplayers",
        "List jailed players"
    )
    public val KILL: PermissionSpec = moderatorNode(
        "kill",
        "Kill a player"
    )
    public val GAMEMODE: PermissionSpec = moderatorNode(
        "gamemode",
        "Change a game mode"
    )
    public val SUDO: PermissionSpec = moderatorNode(
        "sudo",
        "Run a command as another player"
    )
    public val EXP: PermissionSpec = playerNode(
        "exp",
        "View your experience"
    )
    public val EXP_GET: PermissionSpec = moderatorNode(
        "exp.get",
        "View another player's experience"
    )
    public val EXP_GIVE: PermissionSpec = moderatorNode(
        "exp.give",
        "Give experience points"
    )
    public val EXP_TAKE: PermissionSpec = moderatorNode(
        "exp.take",
        "Remove experience points"
    )
    public val EXP_SET: PermissionSpec = moderatorNode(
        "exp.set",
        "Set experience points"
    )
    public val SPEED: PermissionSpec = playerNode(
        "speed",
        "Set your speed multiplier"
    )
    public val SPEED_OTHERS: PermissionSpec = moderatorNode(
        "speed.others",
        "Set another player's speed multiplier"
    )
    public val SOCIAL_SPY: PermissionSpec = moderatorNode(
        "socialspy",
        "Toggle social spy"
    )
    public val VANISH: PermissionSpec = moderatorNode(
        "vanish",
        "Toggle vanish"
    )
    public val BROADCAST: PermissionSpec = moderatorNode(
        "broadcast",
        "Broadcast a message"
    )
    public val BROADCAST_WORLD: PermissionSpec = moderatorNode(
        "broadcastworld",
        "Broadcast to a world"
    )
    public val MOTD: PermissionSpec = playerNode(
        "motd",
        "View the message of the day"
    )
    public val RULES: PermissionSpec = playerNode(
        "rules",
        "View the server rules"
    )
    public val INFO: PermissionSpec = playerNode(
        "info",
        "View the server info page"
    )
    public val CUSTOMTEXT: PermissionSpec = playerNode(
        "customtext",
        "View a custom server page"
    )
    public val ME: PermissionSpec = playerNode(
        "me",
        "Send an action message"
    )
    public val REPAIR: PermissionSpec = moderatorNode(
        "repair",
        "Repair items"
    )
    public val MORE: PermissionSpec = moderatorNode(
        "more",
        "Refill a stack"
    )
    public val INVSEE: PermissionSpec = moderatorNode(
        "invsee",
        "Inspect an inventory"
    )
    public val CREATE_KIT: PermissionSpec = moderatorNode(
        "createkit",
        "Create a kit"
    )
    public val UPDATE_KIT: PermissionSpec = moderatorNode(
        "updatekit",
        "Update a kit"
    )
    public val DEL_KIT: PermissionSpec = moderatorNode(
        "delkit",
        "Delete a kit"
    )
    public val KIT_RESET: PermissionSpec = moderatorNode(
        "kitreset",
        "Reset kit claims"
    )

    /** Every declared node, in declaration order. */
    public val all: List<PermissionSpec> = listOf(
        ROOT,
        ROOT_LANGUAGE,
        ROOT_STATUS,
        ROOT_RELOAD,
        MODERATOR,
        SET_HOME,
        HOME,
        DEL_HOME,
        HOMES,
        RENAME_HOME,
        WARP,
        WARPS,
        SET_WARP,
        DEL_WARP,
        WARP_INFO,
        WARP_INFO_COORDINATES,
        SPAWN,
        SET_SPAWN,
        DEL_SPAWN,
        BACK,
        TP,
        TP_HERE,
        TP_POS,
        TPR,
        SET_TPR,
        TOP,
        BOTTOM,
        JUMP,
        TPA,
        TPA_HERE,
        TP_ACCEPT,
        TP_DENY,
        TP_CANCEL,
        TPA_ALL,
        TPAUTO,
        MSG,
        REPLY,
        IGNORE,
        MSG_TOGGLE,
        MAIL,
        HELP_OP,
        KIT,
        KITS,
        SHOW_KIT,
        ENDER_CHEST,
        DISPOSAL,
        CONDENSE,
        WORKBENCH,
        ANVIL,
        GRINDSTONE,
        STONECUTTER,
        LOOM,
        CARTOGRAPHY_TABLE,
        SMITHING_TABLE,
        GET_POS,
        DEPTH,
        HAT,
        ITEM_NAME,
        ITEM_LORE,
        CLEAR_INVENTORY,
        CLEAR_INVENTORY_OTHERS,
        SEEN,
        PLAY_TIME,
        WHOIS,
        AFK,
        PING,
        PING_OTHERS,
        NEAR,
        NEAR_BYPASS_RADIUS,
        NICK,
        REALNAME,
        NICK_OTHERS,
        PLAY_TIME_OTHERS,
        HEAL,
        FEED,
        FLY,
        GOD,
        KICK,
        KICK_ALL,
        BAN,
        TEMP_BAN,
        UNBAN,
        BAN_IP,
        TEMP_BAN_IP,
        UNBAN_IP,
        MUTE,
        TEMP_MUTE,
        UNMUTE,
        MUTE_INFO,
        SET_JAIL,
        JAIL,
        UNJAIL,
        TOGGLE_JAIL,
        DEL_JAIL,
        JAILS,
        JAILED_PLAYERS,
        KILL,
        GAMEMODE,
        SUDO,
        EXP,
        EXP_GET,
        EXP_GIVE,
        EXP_TAKE,
        EXP_SET,
        SPEED,
        SPEED_OTHERS,
        SOCIAL_SPY,
        VANISH,
        BROADCAST,
        BROADCAST_WORLD,
        MOTD,
        RULES,
        INFO,
        CUSTOMTEXT,
        ME,
        REPAIR,
        MORE,
        INVSEE,
        CREATE_KIT,
        UPDATE_KIT,
        DEL_KIT,
        KIT_RESET,
    )

}
