package top.likoslupus.cellulosesz.core.permission

import top.likoslupus.cellulosesz.core.permission.PermissionNodeFactory.adminNode
import top.likoslupus.cellulosesz.core.permission.PermissionNodeFactory.moderatorNode
import top.likoslupus.cellulosesz.core.permission.PermissionNodeFactory.playerNode

/** Administration command nodes, grouped by the bounded context that owns the commands. */
internal object AdministrationPermissionNodes {

    internal val HEAL: PermissionSpec = moderatorNode(
        "heal",
        "Heal a player"
    )

    internal val FEED: PermissionSpec = moderatorNode(
        "feed",
        "Feed a player"
    )

    internal val FLY: PermissionSpec = moderatorNode(
        "fly",
        "Toggle flight"
    )

    internal val GOD: PermissionSpec = moderatorNode(
        "god",
        "Toggle invulnerability"
    )

    internal val KICK: PermissionSpec = moderatorNode(
        "kick",
        "Kick a player"
    )

    internal val KICK_ALL: PermissionSpec = moderatorNode(
        "kickall",
        "Kick every player"
    )

    internal val BAN: PermissionSpec = moderatorNode(
        "ban",
        "Ban a player"
    )

    internal val TEMP_BAN: PermissionSpec = moderatorNode(
        "tempban",
        "Temporarily ban a player"
    )

    internal val UNBAN: PermissionSpec = moderatorNode(
        "unban",
        "Unban a player"
    )

    internal val BAN_IP: PermissionSpec = moderatorNode(
        "banip",
        "Ban an IP"
    )

    internal val TEMP_BAN_IP: PermissionSpec = moderatorNode(
        "tempbanip",
        "Temporarily ban an IP"
    )

    internal val UNBAN_IP: PermissionSpec = moderatorNode(
        "unbanip",
        "Unban an IP"
    )

    internal val MUTE: PermissionSpec = moderatorNode(
        "mute",
        "Mute a player"
    )

    internal val TEMP_MUTE: PermissionSpec = moderatorNode(
        "tempmute",
        "Temporarily mute a player"
    )

    internal val UNMUTE: PermissionSpec = moderatorNode(
        "unmute",
        "Unmute a player"
    )

    internal val MUTE_INFO: PermissionSpec = moderatorNode(
        "muteinfo",
        "Inspect a mute"
    )

    internal val SET_JAIL: PermissionSpec = moderatorNode(
        "setjail",
        "Create a jail"
    )

    internal val JAIL: PermissionSpec = moderatorNode(
        "jail",
        "Jail a player"
    )

    internal val UNJAIL: PermissionSpec = moderatorNode(
        "unjail",
        "Release a player from jail"
    )

    internal val TOGGLE_JAIL: PermissionSpec = moderatorNode(
        "togglejail",
        "Toggle a player's jail state"
    )

    internal val DEL_JAIL: PermissionSpec = moderatorNode(
        "deljail",
        "Delete a jail"
    )

    internal val JAILS: PermissionSpec = moderatorNode(
        "jails",
        "List jails"
    )

    internal val JAILED_PLAYERS: PermissionSpec = moderatorNode(
        "jailedplayers",
        "List jailed players"
    )

    internal val KILL: PermissionSpec = moderatorNode(
        "kill",
        "Kill a player"
    )

    internal val GAMEMODE: PermissionSpec = moderatorNode(
        "gamemode",
        "Change a game mode"
    )

    internal val SUDO: PermissionSpec = moderatorNode(
        "sudo",
        "Run a command as another player"
    )

    internal val EXP: PermissionSpec = playerNode(
        "exp",
        "View your experience"
    )

    internal val EXP_GET: PermissionSpec = moderatorNode(
        "exp.get",
        "View another player's experience"
    )

    internal val EXP_GIVE: PermissionSpec = moderatorNode(
        "exp.give",
        "Give experience points"
    )

    internal val EXP_TAKE: PermissionSpec = moderatorNode(
        "exp.take",
        "Remove experience points"
    )

    internal val EXP_SET: PermissionSpec = moderatorNode(
        "exp.set",
        "Set experience points"
    )

    internal val SPEED: PermissionSpec = playerNode(
        "speed",
        "Set your speed multiplier"
    )

    internal val SPEED_OTHERS: PermissionSpec = moderatorNode(
        "speed.others",
        "Set another player's speed multiplier"
    )

    internal val SOCIAL_SPY: PermissionSpec = moderatorNode(
        "socialspy",
        "Toggle social spy"
    )

    internal val VANISH: PermissionSpec = moderatorNode(
        "vanish",
        "Toggle vanish"
    )

    internal val SPAWNER: PermissionSpec = moderatorNode(
        "spawner",
        "Change the looked-at spawner"
    )

    internal val SPAWNER_DELAY: PermissionSpec = PermissionSpec(
        "cellulosesz.spawner.delay",
        VanillaPermissionFallback.COMMANDS_MODERATOR,
        "Set a custom spawner delay"
    )

    internal val SPAWN_MOB: PermissionSpec = moderatorNode(
        "spawnmob",
        "Spawn mobs"
    )

    internal val REMOVE: PermissionSpec = adminNode(
        "remove",
        "Remove entities in bulk"
    )

    internal val TIME: PermissionSpec = moderatorNode(
        "time",
        "Set a world's time"
    )

    internal val WEATHER: PermissionSpec = moderatorNode(
        "weather",
        "Set a world's weather"
    )

    internal val P_TIME: PermissionSpec = playerNode(
        "ptime",
        "Set your personal time"
    )

    internal val P_WEATHER: PermissionSpec = playerNode(
        "pweather",
        "Set your personal weather"
    )

    internal val P_TIME_OTHERS: PermissionSpec = PermissionSpec(
        "cellulosesz.ptime.others",
        VanillaPermissionFallback.COMMANDS_MODERATOR,
        "Set another player's personal time"
    )

    internal val P_WEATHER_OTHERS: PermissionSpec = PermissionSpec(
        "cellulosesz.pweather.others",
        VanillaPermissionFallback.COMMANDS_MODERATOR,
        "Set another player's personal weather"
    )

    internal val THUNDER: PermissionSpec = moderatorNode(
        "thunder",
        "Toggle thunder in a world"
    )

    internal val LIGHTNING: PermissionSpec = moderatorNode(
        "lightning",
        "Strike lightning"
    )

    internal val FIREBALL: PermissionSpec = moderatorNode(
        "fireball",
        "Launch a projectile"
    )

    internal val FIREWORK: PermissionSpec = moderatorNode(
        "firework",
        "Launch a firework"
    )

    internal val NUKE: PermissionSpec = adminNode(
        "nuke",
        "Create a large explosion"
    )

    internal val TREE: PermissionSpec = moderatorNode(
        "tree",
        "Grow a tree"
    )

    internal val BIG_TREE: PermissionSpec = moderatorNode(
        "bigtree",
        "Grow a large tree"
    )

    internal val BREAK: PermissionSpec = moderatorNode(
        "break",
        "Break the looked-at block"
    )

    internal val ICE: PermissionSpec = moderatorNode(
        "ice",
        "Freeze nearby water"
    )

    internal val EXT: PermissionSpec = moderatorNode(
        "ext",
        "Extinguish fire"
    )

    internal val ICE_OTHERS: PermissionSpec = PermissionSpec(
        "cellulosesz.ice.others",
        VanillaPermissionFallback.COMMANDS_MODERATOR,
        "Freeze another player"
    )

    internal val EXT_OTHERS: PermissionSpec = PermissionSpec(
        "cellulosesz.ext.others",
        VanillaPermissionFallback.COMMANDS_MODERATOR,
        "Extinguish another player"
    )

    internal val BREAK_BEDROCK: PermissionSpec = PermissionSpec(
        "cellulosesz.break.bedrock",
        VanillaPermissionFallback.COMMANDS_MODERATOR,
        "Break bedrock"
    )

    internal val LIGHTNING_OTHERS: PermissionSpec = PermissionSpec(
        "cellulosesz.lightning.others",
        VanillaPermissionFallback.COMMANDS_MODERATOR,
        "Smite another player"
    )

    internal val FIREBALL_RIDE: PermissionSpec = PermissionSpec(
        "cellulosesz.fireball.ride",
        VanillaPermissionFallback.COMMANDS_MODERATOR,
        "Ride a launched projectile"
    )

    internal val FIREWORK_FIRE: PermissionSpec = PermissionSpec(
        "cellulosesz.firework.fire",
        VanillaPermissionFallback.COMMANDS_MODERATOR,
        "Launch held fireworks"
    )

    internal val SPAWN_MOB_STACK: PermissionSpec = PermissionSpec(
        "cellulosesz.spawnmob.stack",
        VanillaPermissionFallback.COMMANDS_MODERATOR,
        "Spawn stacked or mounted mobs"
    )

    internal val BURN: PermissionSpec = moderatorNode(
        "burn",
        "Set a player on fire"
    )

    internal val GC: PermissionSpec = moderatorNode(
        "gc",
        "Show server resource statistics"
    )

    internal val ANTIOCH: PermissionSpec = moderatorNode(
        "antioch",
        "Spawn primed TNT"
    )

    internal val BEEZOOKA: PermissionSpec = moderatorNode(
        "beezooka",
        "Launch a bee"
    )

    internal val KITTY_CANNON: PermissionSpec = moderatorNode(
        "kittycannon",
        "Launch a cat"
    )

    internal val EDIT_SIGN: PermissionSpec = moderatorNode(
        "editsign",
        "Edit the looked-at sign"
    )

    internal val EDIT_SIGN_COLORS: PermissionSpec = PermissionSpec(
        "cellulosesz.editsign.colors",
        VanillaPermissionFallback.COMMANDS_ADMIN,
        "Use colour codes when editing a sign"
    )

    internal val HEAL_OTHERS: PermissionSpec = PermissionSpec(
        "cellulosesz.heal.others",
        VanillaPermissionFallback.COMMANDS_MODERATOR,
        "Heal another player"
    )

    internal val HEAL_COOLDOWN_BYPASS: PermissionSpec = PermissionSpec(
        "cellulosesz.heal.cooldown.bypass",
        VanillaPermissionFallback.COMMANDS_MODERATOR,
        "Bypass the heal cooldown"
    )

    internal val GOD_OTHERS: PermissionSpec = PermissionSpec(
        "cellulosesz.god.others",
        VanillaPermissionFallback.COMMANDS_MODERATOR,
        "Toggle another player's invulnerability"
    )

    internal val FLY_OTHERS: PermissionSpec = PermissionSpec(
        "cellulosesz.fly.others",
        VanillaPermissionFallback.COMMANDS_MODERATOR,
        "Toggle another player's flight"
    )

    internal val MUTE_UNLIMITED: PermissionSpec = PermissionSpec(
        "cellulosesz.mute.unlimited",
        VanillaPermissionFallback.COMMANDS_ADMIN,
        "Mute beyond the configured maximum duration"
    )

    internal val SILENT_JOIN: PermissionSpec = PermissionSpec(
        "cellulosesz.silentjoin",
        VanillaPermissionFallback.COMMANDS_MODERATOR,
        "Join and quit without announcing it"
    )

    internal val SILENT_JOIN_VANISH: PermissionSpec = PermissionSpec(
        "cellulosesz.silentjoin.vanish",
        VanillaPermissionFallback.COMMANDS_MODERATOR,
        "Enter vanish when joining silently"
    )

    internal val PVP_DELAY_EXEMPT: PermissionSpec = PermissionSpec(
        "cellulosesz.pvpdelay.exempt",
        VanillaPermissionFallback.COMMANDS_MODERATOR,
        "Attack immediately after logging in"
    )

    internal val BACK_ON_DEATH: PermissionSpec = PermissionSpec(
        "cellulosesz.back.ondeath",
        VanillaPermissionFallback.ALLOW_ALL,
        "Use /back to return to your death location"
    )

    internal val BACKUP: PermissionSpec = adminNode(
        "backup",
        "Run a server backup"
    )

}
