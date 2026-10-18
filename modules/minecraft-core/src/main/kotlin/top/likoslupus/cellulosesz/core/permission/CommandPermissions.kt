package top.likoslupus.cellulosesz.core.permission

/**
 * The stable CellulosesZ permission node catalog. Nodes use `cellulosesz.command.<primary>`; a
 * player command still gets a node (fallback ALLOW_ALL) so an admin can explicitly deny it later.
 * Undefined nodes fall back to the exact vanilla behaviour.
 *
 * The declarations are owned by the domain node objects; this object is the single stable facade
 * every call site and the permission inspector use, and it preserves the catalog order in [all].
 */
public object CommandPermissions {

    public const val PREFIX: String = PermissionNodeFactory.PREFIX

    public val ROOT: PermissionSpec = RootPermissionNodes.ROOT
    public val ROOT_LANGUAGE: PermissionSpec = RootPermissionNodes.ROOT_LANGUAGE
    public val ROOT_STATUS: PermissionSpec = RootPermissionNodes.ROOT_STATUS
    public val ROOT_RELOAD: PermissionSpec = RootPermissionNodes.ROOT_RELOAD
    public val ROOT_COMMANDS: PermissionSpec = RootPermissionNodes.ROOT_COMMANDS
    public val ROOT_PERMISSIONS: PermissionSpec = RootPermissionNodes.ROOT_PERMISSIONS
    public val MODERATOR: PermissionSpec = RootPermissionNodes.MODERATOR
    public val SET_HOME: PermissionSpec = MovementPermissionNodes.SET_HOME
    public val HOME: PermissionSpec = MovementPermissionNodes.HOME
    public val DEL_HOME: PermissionSpec = MovementPermissionNodes.DEL_HOME
    public val HOMES: PermissionSpec = MovementPermissionNodes.HOMES
    public val RENAME_HOME: PermissionSpec = MovementPermissionNodes.RENAME_HOME
    public val WARP: PermissionSpec = MovementPermissionNodes.WARP
    public val WARPS: PermissionSpec = MovementPermissionNodes.WARPS
    public val SET_WARP: PermissionSpec = MovementPermissionNodes.SET_WARP
    public val DEL_WARP: PermissionSpec = MovementPermissionNodes.DEL_WARP
    public val WARP_INFO: PermissionSpec = MovementPermissionNodes.WARP_INFO
    public val WARP_INFO_COORDINATES: PermissionSpec = MovementPermissionNodes.WARP_INFO_COORDINATES
    public val SPAWN: PermissionSpec = MovementPermissionNodes.SPAWN
    public val SET_SPAWN: PermissionSpec = MovementPermissionNodes.SET_SPAWN
    public val DEL_SPAWN: PermissionSpec = MovementPermissionNodes.DEL_SPAWN
    public val BACK: PermissionSpec = MovementPermissionNodes.BACK
    public val TP: PermissionSpec = MovementPermissionNodes.TP
    public val TP_VANILLA_OVERRIDE: PermissionSpec = MovementPermissionNodes.TP_VANILLA_OVERRIDE
    public val TP_HERE: PermissionSpec = MovementPermissionNodes.TP_HERE
    public val TP_POS: PermissionSpec = MovementPermissionNodes.TP_POS
    public val TPR: PermissionSpec = MovementPermissionNodes.TPR
    public val SET_TPR: PermissionSpec = MovementPermissionNodes.SET_TPR
    public val TOP: PermissionSpec = MovementPermissionNodes.TOP
    public val BOTTOM: PermissionSpec = MovementPermissionNodes.BOTTOM
    public val JUMP: PermissionSpec = MovementPermissionNodes.JUMP
    public val TPA: PermissionSpec = MovementPermissionNodes.TPA
    public val TPA_HERE: PermissionSpec = MovementPermissionNodes.TPA_HERE
    public val TP_ACCEPT: PermissionSpec = MovementPermissionNodes.TP_ACCEPT
    public val TP_DENY: PermissionSpec = MovementPermissionNodes.TP_DENY
    public val TP_CANCEL: PermissionSpec = MovementPermissionNodes.TP_CANCEL
    public val TPA_ALL: PermissionSpec = MovementPermissionNodes.TPA_ALL
    public val TPAUTO: PermissionSpec = MovementPermissionNodes.TPAUTO
    public val TP_TOGGLE: PermissionSpec = MovementPermissionNodes.TP_TOGGLE
    public val TP_TOGGLE_OTHERS: PermissionSpec = MovementPermissionNodes.TP_TOGGLE_OTHERS
    public val TP_TOGGLE_BYPASS: PermissionSpec = MovementPermissionNodes.TP_TOGGLE_BYPASS
    public val MSG: PermissionSpec = CommunicationPermissionNodes.MSG
    public val REPLY: PermissionSpec = CommunicationPermissionNodes.REPLY
    public val IGNORE: PermissionSpec = CommunicationPermissionNodes.IGNORE
    public val MSG_TOGGLE: PermissionSpec = CommunicationPermissionNodes.MSG_TOGGLE
    public val REPLY_TOGGLE: PermissionSpec = CommunicationPermissionNodes.REPLY_TOGGLE
    public val MAIL: PermissionSpec = CommunicationPermissionNodes.MAIL
    public val HELP_OP: PermissionSpec = CommunicationPermissionNodes.HELP_OP
    public val KIT: PermissionSpec = UtilityPermissionNodes.KIT
    public val KITS: PermissionSpec = UtilityPermissionNodes.KITS
    public val SHOW_KIT: PermissionSpec = UtilityPermissionNodes.SHOW_KIT
    public val ENDER_CHEST: PermissionSpec = UtilityPermissionNodes.ENDER_CHEST
    public val DISPOSAL: PermissionSpec = UtilityPermissionNodes.DISPOSAL
    public val CONDENSE: PermissionSpec = UtilityPermissionNodes.CONDENSE
    public val WORKBENCH: PermissionSpec = UtilityPermissionNodes.WORKBENCH
    public val ANVIL: PermissionSpec = UtilityPermissionNodes.ANVIL
    public val GRINDSTONE: PermissionSpec = UtilityPermissionNodes.GRINDSTONE
    public val STONECUTTER: PermissionSpec = UtilityPermissionNodes.STONECUTTER
    public val LOOM: PermissionSpec = UtilityPermissionNodes.LOOM
    public val CARTOGRAPHY_TABLE: PermissionSpec = UtilityPermissionNodes.CARTOGRAPHY_TABLE
    public val SMITHING_TABLE: PermissionSpec = UtilityPermissionNodes.SMITHING_TABLE
    public val GET_POS: PermissionSpec = UtilityPermissionNodes.GET_POS
    public val DEPTH: PermissionSpec = UtilityPermissionNodes.DEPTH
    public val HAT: PermissionSpec = UtilityPermissionNodes.HAT
    public val ITEM_NAME: PermissionSpec = UtilityPermissionNodes.ITEM_NAME
    public val ITEM_LORE: PermissionSpec = UtilityPermissionNodes.ITEM_LORE
    public val CLEAR_INVENTORY: PermissionSpec = UtilityPermissionNodes.CLEAR_INVENTORY
    public val CLEAR_INVENTORY_OTHERS: PermissionSpec = UtilityPermissionNodes.CLEAR_INVENTORY_OTHERS
    public val CLEAR_INVENTORY_CONFIRM_TOGGLE: PermissionSpec = UtilityPermissionNodes.CLEAR_INVENTORY_CONFIRM_TOGGLE
    public val SEEN: PermissionSpec = PlayerPermissionNodes.SEEN
    public val PLAY_TIME: PermissionSpec = PlayerPermissionNodes.PLAY_TIME
    public val WHOIS: PermissionSpec = PlayerPermissionNodes.WHOIS
    public val AFK: PermissionSpec = PlayerPermissionNodes.AFK
    public val PING: PermissionSpec = PlayerPermissionNodes.PING
    public val PING_OTHERS: PermissionSpec = PlayerPermissionNodes.PING_OTHERS
    public val NEAR: PermissionSpec = PlayerPermissionNodes.NEAR
    public val NEAR_BYPASS_RADIUS: PermissionSpec = PlayerPermissionNodes.NEAR_BYPASS_RADIUS
    public val NICK: PermissionSpec = PlayerPermissionNodes.NICK
    public val REALNAME: PermissionSpec = PlayerPermissionNodes.REALNAME
    public val NICK_OTHERS: PermissionSpec = PlayerPermissionNodes.NICK_OTHERS
    public val PLAY_TIME_OTHERS: PermissionSpec = PlayerPermissionNodes.PLAY_TIME_OTHERS
    public val HEAL: PermissionSpec = AdministrationPermissionNodes.HEAL
    public val FEED: PermissionSpec = AdministrationPermissionNodes.FEED
    public val FLY: PermissionSpec = AdministrationPermissionNodes.FLY
    public val GOD: PermissionSpec = AdministrationPermissionNodes.GOD
    public val KICK: PermissionSpec = AdministrationPermissionNodes.KICK
    public val KICK_ALL: PermissionSpec = AdministrationPermissionNodes.KICK_ALL
    public val BAN: PermissionSpec = AdministrationPermissionNodes.BAN
    public val TEMP_BAN: PermissionSpec = AdministrationPermissionNodes.TEMP_BAN
    public val UNBAN: PermissionSpec = AdministrationPermissionNodes.UNBAN
    public val BAN_IP: PermissionSpec = AdministrationPermissionNodes.BAN_IP
    public val TEMP_BAN_IP: PermissionSpec = AdministrationPermissionNodes.TEMP_BAN_IP
    public val UNBAN_IP: PermissionSpec = AdministrationPermissionNodes.UNBAN_IP
    public val MUTE: PermissionSpec = AdministrationPermissionNodes.MUTE
    public val TEMP_MUTE: PermissionSpec = AdministrationPermissionNodes.TEMP_MUTE
    public val UNMUTE: PermissionSpec = AdministrationPermissionNodes.UNMUTE
    public val MUTE_INFO: PermissionSpec = AdministrationPermissionNodes.MUTE_INFO
    public val SET_JAIL: PermissionSpec = AdministrationPermissionNodes.SET_JAIL
    public val JAIL: PermissionSpec = AdministrationPermissionNodes.JAIL
    public val UNJAIL: PermissionSpec = AdministrationPermissionNodes.UNJAIL
    public val TOGGLE_JAIL: PermissionSpec = AdministrationPermissionNodes.TOGGLE_JAIL
    public val DEL_JAIL: PermissionSpec = AdministrationPermissionNodes.DEL_JAIL
    public val JAILS: PermissionSpec = AdministrationPermissionNodes.JAILS
    public val JAILED_PLAYERS: PermissionSpec = AdministrationPermissionNodes.JAILED_PLAYERS
    public val KILL: PermissionSpec = AdministrationPermissionNodes.KILL
    public val GAMEMODE: PermissionSpec = AdministrationPermissionNodes.GAMEMODE
    public val SUDO: PermissionSpec = AdministrationPermissionNodes.SUDO
    public val EXP: PermissionSpec = AdministrationPermissionNodes.EXP
    public val EXP_GET: PermissionSpec = AdministrationPermissionNodes.EXP_GET
    public val EXP_GIVE: PermissionSpec = AdministrationPermissionNodes.EXP_GIVE
    public val EXP_TAKE: PermissionSpec = AdministrationPermissionNodes.EXP_TAKE
    public val EXP_SET: PermissionSpec = AdministrationPermissionNodes.EXP_SET
    public val SPEED: PermissionSpec = AdministrationPermissionNodes.SPEED
    public val SPEED_OTHERS: PermissionSpec = AdministrationPermissionNodes.SPEED_OTHERS
    public val SOCIAL_SPY: PermissionSpec = AdministrationPermissionNodes.SOCIAL_SPY
    public val VANISH: PermissionSpec = AdministrationPermissionNodes.VANISH
    public val BROADCAST: PermissionSpec = CommunicationPermissionNodes.BROADCAST
    public val BROADCAST_WORLD: PermissionSpec = CommunicationPermissionNodes.BROADCAST_WORLD
    public val MOTD: PermissionSpec = CommunicationPermissionNodes.MOTD
    public val RULES: PermissionSpec = CommunicationPermissionNodes.RULES
    public val INFO: PermissionSpec = CommunicationPermissionNodes.INFO
    public val CUSTOMTEXT: PermissionSpec = CommunicationPermissionNodes.CUSTOMTEXT
    public val ME: PermissionSpec = CommunicationPermissionNodes.ME
    public val REPAIR: PermissionSpec = UtilityPermissionNodes.REPAIR
    public val MORE: PermissionSpec = UtilityPermissionNodes.MORE
    public val INVSEE: PermissionSpec = UtilityPermissionNodes.INVSEE
    public val CREATE_KIT: PermissionSpec = UtilityPermissionNodes.CREATE_KIT
    public val UPDATE_KIT: PermissionSpec = UtilityPermissionNodes.UPDATE_KIT
    public val DEL_KIT: PermissionSpec = UtilityPermissionNodes.DEL_KIT
    public val KIT_RESET: PermissionSpec = UtilityPermissionNodes.KIT_RESET
    public val BALANCE: PermissionSpec = EconomyPermissionNodes.BALANCE
    public val BALANCE_OTHERS: PermissionSpec = EconomyPermissionNodes.BALANCE_OTHERS
    public val BALANCETOP: PermissionSpec = EconomyPermissionNodes.BALANCETOP
    public val PAY: PermissionSpec = EconomyPermissionNodes.PAY
    public val PAY_TOGGLE: PermissionSpec = EconomyPermissionNodes.PAY_TOGGLE
    public val PAY_TOGGLE_OTHERS: PermissionSpec = EconomyPermissionNodes.PAY_TOGGLE_OTHERS
    public val PAY_TOGGLE_BYPASS: PermissionSpec = EconomyPermissionNodes.PAY_TOGGLE_BYPASS
    public val PAY_CONFIRM_TOGGLE: PermissionSpec = EconomyPermissionNodes.PAY_CONFIRM_TOGGLE
    public val TRANSACTIONS: PermissionSpec = EconomyPermissionNodes.TRANSACTIONS
    public val ECO: PermissionSpec = EconomyPermissionNodes.ECO
    public val ECO_GIVE: PermissionSpec = EconomyPermissionNodes.ECO_GIVE
    public val ECO_TAKE: PermissionSpec = EconomyPermissionNodes.ECO_TAKE
    public val ECO_SET: PermissionSpec = EconomyPermissionNodes.ECO_SET
    public val ECO_HISTORY: PermissionSpec = EconomyPermissionNodes.ECO_HISTORY
    public val ECO_INFO: PermissionSpec = EconomyPermissionNodes.ECO_INFO
    public val ECONOMY_PAY_CAPABILITY: PermissionSpec = EconomyPermissionNodes.ECONOMY_PAY_CAPABILITY
    public val WORLD: PermissionSpec = MovementPermissionNodes.WORLD
    public val TP_O: PermissionSpec = MovementPermissionNodes.TP_O
    public val TP_O_HERE: PermissionSpec = MovementPermissionNodes.TP_O_HERE
    public val TP_OFFLINE: PermissionSpec = MovementPermissionNodes.TP_OFFLINE
    public val TP_ALL: PermissionSpec = MovementPermissionNodes.TP_ALL
    public val COMPASS: PermissionSpec = MovementPermissionNodes.COMPASS
    public val WORTH: PermissionSpec = EconomyPermissionNodes.WORTH
    public val SELL: PermissionSpec = EconomyPermissionNodes.SELL
    public val SET_WORTH: PermissionSpec = EconomyPermissionNodes.SET_WORTH
    public val SELL_BULK: PermissionSpec = EconomyPermissionNodes.SELL_BULK
    public val GIVE: PermissionSpec = UtilityPermissionNodes.GIVE
    public val ITEM: PermissionSpec = UtilityPermissionNodes.ITEM
    public val ITEM_DB: PermissionSpec = UtilityPermissionNodes.ITEM_DB
    public val ENCHANT: PermissionSpec = UtilityPermissionNodes.ENCHANT
    public val ENCHANT_UNSAFE: PermissionSpec = UtilityPermissionNodes.ENCHANT_UNSAFE
    public val POTION: PermissionSpec = UtilityPermissionNodes.POTION
    public val POTION_APPLY: PermissionSpec = UtilityPermissionNodes.POTION_APPLY
    public val POTION_OTHERS: PermissionSpec = UtilityPermissionNodes.POTION_OTHERS
    public val SKULL: PermissionSpec = UtilityPermissionNodes.SKULL
    public val SKULL_OTHERS: PermissionSpec = UtilityPermissionNodes.SKULL_OTHERS
    public val SKULL_SPAWN: PermissionSpec = UtilityPermissionNodes.SKULL_SPAWN
    public val UNLIMITED: PermissionSpec = UtilityPermissionNodes.UNLIMITED
    public val UNLIMITED_OTHERS: PermissionSpec = UtilityPermissionNodes.UNLIMITED_OTHERS
    public val BOOK: PermissionSpec = UtilityPermissionNodes.BOOK
    public val RECIPE: PermissionSpec = UtilityPermissionNodes.RECIPE
    public val POWERTOOL: PermissionSpec = UtilityPermissionNodes.POWERTOOL
    public val POWERTOOL_LIST: PermissionSpec = UtilityPermissionNodes.POWERTOOL_LIST
    public val POWERTOOL_TOGGLE: PermissionSpec = UtilityPermissionNodes.POWERTOOL_TOGGLE
    public val ENDER_CHEST_OTHERS: PermissionSpec = UtilityPermissionNodes.ENDER_CHEST_OTHERS
    public val INVSEE_EDIT: PermissionSpec = UtilityPermissionNodes.INVSEE_EDIT
    public val INVSEE_EQUIP: PermissionSpec = UtilityPermissionNodes.INVSEE_EQUIP
    public val SPAWNER: PermissionSpec = AdministrationPermissionNodes.SPAWNER
    public val SPAWNER_DELAY: PermissionSpec = AdministrationPermissionNodes.SPAWNER_DELAY
    public val SPAWN_MOB: PermissionSpec = AdministrationPermissionNodes.SPAWN_MOB
    public val REMOVE: PermissionSpec = AdministrationPermissionNodes.REMOVE
    public val TIME: PermissionSpec = AdministrationPermissionNodes.TIME
    public val WEATHER: PermissionSpec = AdministrationPermissionNodes.WEATHER
    public val P_TIME: PermissionSpec = AdministrationPermissionNodes.P_TIME
    public val P_WEATHER: PermissionSpec = AdministrationPermissionNodes.P_WEATHER
    public val P_TIME_OTHERS: PermissionSpec = AdministrationPermissionNodes.P_TIME_OTHERS
    public val P_WEATHER_OTHERS: PermissionSpec = AdministrationPermissionNodes.P_WEATHER_OTHERS
    public val THUNDER: PermissionSpec = AdministrationPermissionNodes.THUNDER
    public val LIGHTNING: PermissionSpec = AdministrationPermissionNodes.LIGHTNING
    public val FIREBALL: PermissionSpec = AdministrationPermissionNodes.FIREBALL
    public val FIREWORK: PermissionSpec = AdministrationPermissionNodes.FIREWORK
    public val NUKE: PermissionSpec = AdministrationPermissionNodes.NUKE
    public val TREE: PermissionSpec = AdministrationPermissionNodes.TREE
    public val BIG_TREE: PermissionSpec = AdministrationPermissionNodes.BIG_TREE
    public val BREAK: PermissionSpec = AdministrationPermissionNodes.BREAK
    public val ICE: PermissionSpec = AdministrationPermissionNodes.ICE
    public val EXT: PermissionSpec = AdministrationPermissionNodes.EXT
    public val BURN: PermissionSpec = AdministrationPermissionNodes.BURN
    public val GC: PermissionSpec = AdministrationPermissionNodes.GC
    public val ANTIOCH: PermissionSpec = AdministrationPermissionNodes.ANTIOCH
    public val BEEZOOKA: PermissionSpec = AdministrationPermissionNodes.BEEZOOKA
    public val KITTY_CANNON: PermissionSpec = AdministrationPermissionNodes.KITTY_CANNON
    public val EDIT_SIGN: PermissionSpec = AdministrationPermissionNodes.EDIT_SIGN
    public val EDIT_SIGN_COLORS: PermissionSpec = AdministrationPermissionNodes.EDIT_SIGN_COLORS
    public val LIST: PermissionSpec = PlayerPermissionNodes.LIST
    public val LIST_HIDDEN: PermissionSpec = PlayerPermissionNodes.LIST_HIDDEN
    public val WHOIS_IP: PermissionSpec = PlayerPermissionNodes.WHOIS_IP
    public val AFK_OTHERS: PermissionSpec = PlayerPermissionNodes.AFK_OTHERS
    public val AFK_BYPASS: PermissionSpec = PlayerPermissionNodes.AFK_BYPASS
    public val NICK_COLORS: PermissionSpec = PlayerPermissionNodes.NICK_COLORS
    public val HEAL_OTHERS: PermissionSpec = AdministrationPermissionNodes.HEAL_OTHERS
    public val HEAL_COOLDOWN_BYPASS: PermissionSpec = AdministrationPermissionNodes.HEAL_COOLDOWN_BYPASS
    public val GOD_OTHERS: PermissionSpec = AdministrationPermissionNodes.GOD_OTHERS
    public val FLY_OTHERS: PermissionSpec = AdministrationPermissionNodes.FLY_OTHERS
    public val MUTE_UNLIMITED: PermissionSpec = AdministrationPermissionNodes.MUTE_UNLIMITED
    public val SILENT_JOIN: PermissionSpec = AdministrationPermissionNodes.SILENT_JOIN
    public val SILENT_JOIN_VANISH: PermissionSpec = AdministrationPermissionNodes.SILENT_JOIN_VANISH
    public val PVP_DELAY_EXEMPT: PermissionSpec = AdministrationPermissionNodes.PVP_DELAY_EXEMPT
    public val BACK_ON_DEATH: PermissionSpec = AdministrationPermissionNodes.BACK_ON_DEATH
    public val COMMAND_COST_EXEMPT: PermissionSpec = CommandPermissionNodes.COMMAND_COST_EXEMPT
    public val COMMAND_COOLDOWN_EXEMPT: PermissionSpec = CommandPermissionNodes.COMMAND_COOLDOWN_EXEMPT
    public val BACKUP: PermissionSpec = AdministrationPermissionNodes.BACKUP

    public val all: List<PermissionSpec> = listOf(
        ROOT,
        ROOT_LANGUAGE,
        ROOT_STATUS,
        ROOT_RELOAD,
        ROOT_COMMANDS,
        ROOT_PERMISSIONS,
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
        TP_VANILLA_OVERRIDE,
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
        TP_TOGGLE,
        TP_TOGGLE_OTHERS,
        TP_TOGGLE_BYPASS,
        MSG,
        REPLY,
        IGNORE,
        MSG_TOGGLE,
        REPLY_TOGGLE,
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
        CLEAR_INVENTORY_CONFIRM_TOGGLE,
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
        BALANCE,
        BALANCE_OTHERS,
        BALANCETOP,
        PAY,
        PAY_TOGGLE,
        PAY_TOGGLE_OTHERS,
        PAY_TOGGLE_BYPASS,
        PAY_CONFIRM_TOGGLE,
        TRANSACTIONS,
        ECO,
        ECO_GIVE,
        ECO_TAKE,
        ECO_SET,
        ECO_HISTORY,
        ECO_INFO,
        ECONOMY_PAY_CAPABILITY,
        WORLD,
        TP_O,
        TP_O_HERE,
        TP_OFFLINE,
        TP_ALL,
        COMPASS,
        WORTH,
        SELL,
        SET_WORTH,
        SELL_BULK,
        GIVE,
        ITEM,
        ITEM_DB,
        ENCHANT,
        ENCHANT_UNSAFE,
        POTION,
        POTION_APPLY,
        POTION_OTHERS,
        SKULL,
        SKULL_OTHERS,
        SKULL_SPAWN,
        UNLIMITED,
        UNLIMITED_OTHERS,
        BOOK,
        RECIPE,
        POWERTOOL,
        POWERTOOL_LIST,
        POWERTOOL_TOGGLE,
        ENDER_CHEST_OTHERS,
        INVSEE_EDIT,
        INVSEE_EQUIP,
        SPAWNER,
        SPAWNER_DELAY,
        SPAWN_MOB,
        REMOVE,
        TIME,
        WEATHER,
        P_TIME,
        P_WEATHER,
        P_TIME_OTHERS,
        P_WEATHER_OTHERS,
        THUNDER,
        LIGHTNING,
        FIREBALL,
        FIREWORK,
        NUKE,
        TREE,
        BIG_TREE,
        BREAK,
        ICE,
        EXT,
        BURN,
        GC,
        ANTIOCH,
        BEEZOOKA,
        KITTY_CANNON,
        EDIT_SIGN,
        EDIT_SIGN_COLORS,
        LIST,
        LIST_HIDDEN,
        WHOIS_IP,
        AFK_OTHERS,
        AFK_BYPASS,
        NICK_COLORS,
        HEAL_OTHERS,
        HEAL_COOLDOWN_BYPASS,
        GOD_OTHERS,
        FLY_OTHERS,
        MUTE_UNLIMITED,
        SILENT_JOIN,
        SILENT_JOIN_VANISH,
        PVP_DELAY_EXEMPT,
        BACK_ON_DEATH,
        COMMAND_COST_EXEMPT,
        COMMAND_COOLDOWN_EXEMPT,
        BACKUP,
    )

}
