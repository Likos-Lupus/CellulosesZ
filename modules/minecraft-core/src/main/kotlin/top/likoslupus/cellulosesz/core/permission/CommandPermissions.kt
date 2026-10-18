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

    private fun adminNode(name: String, description: String): PermissionSpec =
        PermissionSpec(
            PREFIX + name,
            VanillaPermissionFallback.COMMANDS_ADMIN,
            description
        )

    public val ROOT: PermissionSpec = playerNode(
        "cellulosesz",
        "Use the CellulosesZ command"
    )

    public val ROOT_LANGUAGE: PermissionSpec = PermissionSpec(
        "cellulosesz.command.cellulosesz.language",
        VanillaPermissionFallback.ALLOW_ALL,
        "Change your CellulosesZ language"
    )

    public val ROOT_STATUS: PermissionSpec = PermissionSpec(
        "cellulosesz.command.cellulosesz.status",
        VanillaPermissionFallback.COMMANDS_MODERATOR,
        "View CellulosesZ status"
    )

    public val ROOT_RELOAD: PermissionSpec = PermissionSpec(
        "cellulosesz.command.cellulosesz.reload",
        VanillaPermissionFallback.COMMANDS_MODERATOR,
        "Reload CellulosesZ configuration"
    )

    public val ROOT_COMMANDS: PermissionSpec = PermissionSpec(
        "cellulosesz.command.cellulosesz.commands",
        VanillaPermissionFallback.COMMANDS_MODERATOR,
        "Inspect CellulosesZ command routes"
    )

    public val ROOT_PERMISSIONS: PermissionSpec = PermissionSpec(
        "cellulosesz.command.cellulosesz.permissions",
        VanillaPermissionFallback.COMMANDS_MODERATOR,
        "Inspect CellulosesZ permission nodes"
    )

    /** Internal capability used by moderator notices and staff broadcasts (not a command node). */
    public val MODERATOR: PermissionSpec = PermissionSpec(
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

    /**
     * The extra gate for using the CellulosesZ implementation through the *vanilla* `/tp` entry
     * when takeover is enabled. The ordinary `/cztp` and `/cellulosesz:tp` entries keep using [TP]
     * only, so taking over the vanilla literal never widens an ordinary player's access.
     */
    public val TP_VANILLA_OVERRIDE: PermissionSpec = PermissionSpec(
        "cellulosesz.command.override.tp",
        VanillaPermissionFallback.COMMANDS_GAMEMASTER,
        "Use the CellulosesZ implementation through the vanilla /tp entry",
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
    public val TP_TOGGLE: PermissionSpec = playerNode(
        "tptoggle",
        "Toggle accepting teleport requests"
    )

    /** Toggling another player's teleport setting; a non-command capability. */
    public val TP_TOGGLE_OTHERS: PermissionSpec = PermissionSpec(
        "cellulosesz.tptoggle.others",
        VanillaPermissionFallback.COMMANDS_MODERATOR,
        "Toggle another player's teleport setting"
    )

    /** Teleporting to or moving a player who disabled teleports; a non-command capability. */
    public val TP_TOGGLE_BYPASS: PermissionSpec = PermissionSpec(
        "cellulosesz.tptoggle.bypass",
        VanillaPermissionFallback.COMMANDS_MODERATOR,
        "Teleport regardless of a player's teleport setting"
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
    public val REPLY_TOGGLE: PermissionSpec = playerNode(
        "rtoggle",
        "Toggle replying to the last recipient"
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
    public val CLEAR_INVENTORY_CONFIRM_TOGGLE: PermissionSpec = playerNode(
        "clearinventoryconfirmtoggle",
        "Toggle the clear-inventory confirmation"
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

    public val BALANCE: PermissionSpec = playerNode(
        "balance",
        "View your balance"
    )
    public val BALANCE_OTHERS: PermissionSpec = moderatorNode(
        "balance.others",
        "View another player's balance"
    )
    public val BALANCETOP: PermissionSpec = playerNode(
        "balancetop",
        "View the balance ranking"
    )
    public val PAY: PermissionSpec = playerNode(
        "pay",
        "Pay another player"
    )
    public val PAY_TOGGLE: PermissionSpec = playerNode(
        "paytoggle",
        "Toggle accepting payments"
    )

    /** Toggling another player's payment setting; a non-command capability. */
    public val PAY_TOGGLE_OTHERS: PermissionSpec = PermissionSpec(
        "cellulosesz.paytoggle.others",
        VanillaPermissionFallback.COMMANDS_MODERATOR,
        "Toggle another player's payment setting"
    )

    /** Paying a player who disabled payments; a non-command capability. */
    public val PAY_TOGGLE_BYPASS: PermissionSpec = PermissionSpec(
        "cellulosesz.paytoggle.bypass",
        VanillaPermissionFallback.COMMANDS_MODERATOR,
        "Pay a player who has disabled payments"
    )
    public val PAY_CONFIRM_TOGGLE: PermissionSpec = playerNode(
        "payconfirmtoggle",
        "Toggle payment confirmation"
    )
    public val TRANSACTIONS: PermissionSpec = playerNode(
        "transactions",
        "View your transaction history"
    )
    public val ECO: PermissionSpec = moderatorNode(
        "eco",
        "Use the economy administration command"
    )
    public val ECO_GIVE: PermissionSpec = adminNode(
        "eco.give",
        "Grant currency to a player"
    )
    public val ECO_TAKE: PermissionSpec = adminNode(
        "eco.take",
        "Reclaim currency from a player"
    )
    public val ECO_SET: PermissionSpec = adminNode(
        "eco.set",
        "Set a player's balance"
    )
    public val ECO_HISTORY: PermissionSpec = moderatorNode(
        "eco.history",
        "Audit a player's transactions"
    )
    public val ECO_INFO: PermissionSpec = moderatorNode(
        "eco.info",
        "View economy diagnostics"
    )

    /** The player payment capability reused by integrations that act on a player's behalf. */
    public val ECONOMY_PAY_CAPABILITY: PermissionSpec = PermissionSpec(
        "cellulosesz.economy.pay",
        VanillaPermissionFallback.ALLOW_ALL,
        "Use the pay capability through integrations"
    )

    /** Every declared node, in declaration order. */
    public val WORLD: PermissionSpec = playerNode("world", "Switch worlds")

    public val TP_O: PermissionSpec = moderatorNode("tpo", "Teleport directly to a player")

    public val TP_O_HERE: PermissionSpec = moderatorNode(
        "tpohere",
        "Teleport a player to you directly"
    )

    public val TP_OFFLINE: PermissionSpec = moderatorNode(
        "tpoffline",
        "Teleport to an offline player's last known location"
    )

    public val TP_ALL: PermissionSpec = adminNode("tpall", "Teleport every player to a target")

    public val COMPASS: PermissionSpec = playerNode("compass", "Show your current bearing")

    public val WORTH: PermissionSpec = playerNode("worth", "Look up an item's worth")

    public val SELL: PermissionSpec = playerNode("sell", "Sell items to the server")

    public val SET_WORTH: PermissionSpec = moderatorNode("setworth", "Set an item's worth")

    public val SELL_BULK: PermissionSpec = PermissionSpec(
        "cellulosesz.sell.bulk",
        VanillaPermissionFallback.COMMANDS_MODERATOR,
        "Sell an entire inventory in one command"
    )

    public val GIVE: PermissionSpec = moderatorNode("give", "Give items to players")

    public val ITEM: PermissionSpec = moderatorNode("item", "Give yourself an item")

    public val ITEM_DB: PermissionSpec = playerNode("itemdb", "Look up item ids and aliases")

    public val ENCHANT: PermissionSpec = moderatorNode("enchant", "Enchant the held item")

    public val ENCHANT_UNSAFE: PermissionSpec = PermissionSpec(
        "cellulosesz.enchant.unsafe",
        VanillaPermissionFallback.COMMANDS_ADMIN,
        "Use enchantment levels above the vanilla maximum"
    )

    public val POTION: PermissionSpec = moderatorNode("potion", "Edit the held potion")

    public val POTION_APPLY: PermissionSpec = PermissionSpec(
        "cellulosesz.potion.apply",
        VanillaPermissionFallback.COMMANDS_MODERATOR,
        "Apply the effects of the held potion to yourself"
    )

    public val POTION_OTHERS: PermissionSpec = PermissionSpec(
        "cellulosesz.potion.others",
        VanillaPermissionFallback.COMMANDS_MODERATOR,
        "Edit another player's potion"
    )

    public val SKULL: PermissionSpec = moderatorNode("skull", "Create a player head")

    public val SKULL_OTHERS: PermissionSpec = PermissionSpec(
        "cellulosesz.skull.others",
        VanillaPermissionFallback.COMMANDS_MODERATOR,
        "Create a head for another owner"
    )

    public val SKULL_SPAWN: PermissionSpec = PermissionSpec(
        "cellulosesz.skull.spawn",
        VanillaPermissionFallback.COMMANDS_MODERATOR,
        "Spawn a new head item instead of editing the held one"
    )

    public val UNLIMITED: PermissionSpec = adminNode("unlimited", "Toggle unlimited item use")

    public val UNLIMITED_OTHERS: PermissionSpec = PermissionSpec(
        "cellulosesz.unlimited.others",
        VanillaPermissionFallback.COMMANDS_ADMIN,
        "Toggle unlimited item use for another player"
    )

    public val BOOK: PermissionSpec = playerNode("book", "Edit the book in your hand")

    public val RECIPE: PermissionSpec = playerNode("recipe", "Show an item's crafting recipe")

    public val POWERTOOL: PermissionSpec = playerNode(
        "powertool",
        "Bind a command to the held item"
    )

    public val POWERTOOL_LIST: PermissionSpec = playerNode("powertoollist", "List your powertools")

    public val POWERTOOL_TOGGLE: PermissionSpec = playerNode(
        "powertooltoggle",
        "Toggle your powertools on or off"
    )

    public val ENDER_CHEST_OTHERS: PermissionSpec = PermissionSpec(
        "cellulosesz.enderchest.others",
        VanillaPermissionFallback.COMMANDS_MODERATOR,
        "Open another player's ender chest"
    )

    public val INVSEE_EDIT: PermissionSpec = PermissionSpec(
        "cellulosesz.invsee.edit",
        VanillaPermissionFallback.COMMANDS_ADMIN,
        "Edit the inspected inventory"
    )

    public val INVSEE_EQUIP: PermissionSpec = PermissionSpec(
        "cellulosesz.invsee.equip",
        VanillaPermissionFallback.COMMANDS_MODERATOR,
        "Inspect a player's equipment"
    )

    public val SPAWNER: PermissionSpec = moderatorNode("spawner", "Change the looked-at spawner")

    public val SPAWNER_DELAY: PermissionSpec = PermissionSpec(
        "cellulosesz.spawner.delay",
        VanillaPermissionFallback.COMMANDS_MODERATOR,
        "Set a custom spawner delay"
    )

    public val SPAWN_MOB: PermissionSpec = moderatorNode("spawnmob", "Spawn mobs")

    public val REMOVE: PermissionSpec = adminNode("remove", "Remove entities in bulk")

    public val TIME: PermissionSpec = moderatorNode("time", "Set a world's time")

    public val WEATHER: PermissionSpec = moderatorNode("weather", "Set a world's weather")

    public val P_TIME: PermissionSpec = playerNode("ptime", "Set your personal time")

    public val P_WEATHER: PermissionSpec = playerNode("pweather", "Set your personal weather")

    public val P_TIME_OTHERS: PermissionSpec = PermissionSpec(
        "cellulosesz.ptime.others",
        VanillaPermissionFallback.COMMANDS_MODERATOR,
        "Set another player's personal time"
    )

    public val P_WEATHER_OTHERS: PermissionSpec = PermissionSpec(
        "cellulosesz.pweather.others",
        VanillaPermissionFallback.COMMANDS_MODERATOR,
        "Set another player's personal weather"
    )

    public val THUNDER: PermissionSpec = moderatorNode("thunder", "Toggle thunder in a world")

    public val LIGHTNING: PermissionSpec = moderatorNode("lightning", "Strike lightning")

    public val FIREBALL: PermissionSpec = moderatorNode("fireball", "Launch a projectile")

    public val FIREWORK: PermissionSpec = moderatorNode("firework", "Launch a firework")

    public val NUKE: PermissionSpec = adminNode("nuke", "Create a large explosion")

    public val TREE: PermissionSpec = moderatorNode("tree", "Grow a tree")

    public val BIG_TREE: PermissionSpec = moderatorNode("bigtree", "Grow a large tree")

    public val BREAK: PermissionSpec = moderatorNode("break", "Break the looked-at block")

    public val ICE: PermissionSpec = moderatorNode("ice", "Freeze nearby water")

    public val EXT: PermissionSpec = moderatorNode("ext", "Extinguish fire")

    public val BURN: PermissionSpec = moderatorNode("burn", "Set a player on fire")

    public val GC: PermissionSpec = moderatorNode("gc", "Show server resource statistics")

    public val ANTIOCH: PermissionSpec = moderatorNode("antioch", "Spawn primed TNT")

    public val BEEZOOKA: PermissionSpec = moderatorNode("beezooka", "Launch a bee")

    public val KITTY_CANNON: PermissionSpec = moderatorNode("kittycannon", "Launch a cat")

    public val EDIT_SIGN: PermissionSpec = moderatorNode("editsign", "Edit the looked-at sign")

    public val EDIT_SIGN_COLORS: PermissionSpec = PermissionSpec(
        "cellulosesz.editsign.colors",
        VanillaPermissionFallback.COMMANDS_ADMIN,
        "Use colour codes when editing a sign"
    )

    public val LIST: PermissionSpec = playerNode("list", "List online players")

    public val LIST_HIDDEN: PermissionSpec = PermissionSpec(
        "cellulosesz.list.hidden",
        VanillaPermissionFallback.COMMANDS_MODERATOR,
        "See vanished players in the list"
    )

    public val WHOIS_IP: PermissionSpec = PermissionSpec(
        "cellulosesz.whois.ip",
        VanillaPermissionFallback.COMMANDS_ADMIN,
        "See a player's IP address"
    )

    public val AFK_OTHERS: PermissionSpec = PermissionSpec(
        "cellulosesz.afk.others",
        VanillaPermissionFallback.COMMANDS_MODERATOR,
        "Toggle another player's AFK state"
    )

    public val AFK_BYPASS: PermissionSpec = PermissionSpec(
        "cellulosesz.afk.bypass",
        VanillaPermissionFallback.COMMANDS_MODERATOR,
        "Never be marked AFK automatically"
    )

    public val NICK_COLORS: PermissionSpec = PermissionSpec(
        "cellulosesz.nick.colors",
        VanillaPermissionFallback.COMMANDS_MODERATOR,
        "Use colour codes in your nickname"
    )

    public val HEAL_OTHERS: PermissionSpec = PermissionSpec(
        "cellulosesz.heal.others",
        VanillaPermissionFallback.COMMANDS_MODERATOR,
        "Heal another player"
    )

    public val HEAL_COOLDOWN_BYPASS: PermissionSpec = PermissionSpec(
        "cellulosesz.heal.cooldown.bypass",
        VanillaPermissionFallback.COMMANDS_MODERATOR,
        "Bypass the heal cooldown"
    )

    public val GOD_OTHERS: PermissionSpec = PermissionSpec(
        "cellulosesz.god.others",
        VanillaPermissionFallback.COMMANDS_MODERATOR,
        "Toggle another player's invulnerability"
    )

    public val FLY_OTHERS: PermissionSpec = PermissionSpec(
        "cellulosesz.fly.others",
        VanillaPermissionFallback.COMMANDS_MODERATOR,
        "Toggle another player's flight"
    )

    public val MUTE_UNLIMITED: PermissionSpec = PermissionSpec(
        "cellulosesz.mute.unlimited",
        VanillaPermissionFallback.COMMANDS_ADMIN,
        "Mute beyond the configured maximum duration"
    )

    public val SILENT_JOIN: PermissionSpec = PermissionSpec(
        "cellulosesz.silentjoin",
        VanillaPermissionFallback.COMMANDS_MODERATOR,
        "Join and quit without announcing it"
    )

    public val SILENT_JOIN_VANISH: PermissionSpec = PermissionSpec(
        "cellulosesz.silentjoin.vanish",
        VanillaPermissionFallback.COMMANDS_MODERATOR,
        "Enter vanish when joining silently"
    )

    public val PVP_DELAY_EXEMPT: PermissionSpec = PermissionSpec(
        "cellulosesz.pvpdelay.exempt",
        VanillaPermissionFallback.COMMANDS_MODERATOR,
        "Attack immediately after logging in"
    )

    public val BACK_ON_DEATH: PermissionSpec = PermissionSpec(
        "cellulosesz.back.ondeath",
        VanillaPermissionFallback.ALLOW_ALL,
        "Use /back to return to your death location"
    )

    public val COMMAND_COST_EXEMPT: PermissionSpec = PermissionSpec(
        "cellulosesz.command.cost.exempt",
        VanillaPermissionFallback.COMMANDS_MODERATOR,
        "Run commands without paying their configured cost"
    )

    public val COMMAND_COOLDOWN_EXEMPT: PermissionSpec = PermissionSpec(
        "cellulosesz.command.cooldown.exempt",
        VanillaPermissionFallback.COMMANDS_MODERATOR,
        "Run commands without their configured cooldown"
    )

    public val BACKUP: PermissionSpec = adminNode("backup", "Run a server backup")

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
