package top.likoslupus.cellulosesz.core.permission

import top.likoslupus.cellulosesz.core.permission.PermissionNodeFactory.moderatorNode
import top.likoslupus.cellulosesz.core.permission.PermissionNodeFactory.playerNode

/** Player command nodes, grouped by the bounded context that owns the commands. */
internal object PlayerPermissionNodes {

    internal val SEEN: PermissionSpec = playerNode(
        "seen",
        "View a player's last seen time"
    )

    internal val PLAY_TIME: PermissionSpec = playerNode(
        "playtime",
        "View your play time"
    )

    internal val WHOIS: PermissionSpec = playerNode(
        "whois",
        "View player information"
    )

    internal val AFK: PermissionSpec = playerNode(
        "afk",
        "Toggle AFK"
    )

    internal val PING: PermissionSpec = playerNode(
        "ping",
        "View your latency"
    )

    internal val PING_OTHERS: PermissionSpec = moderatorNode(
        "ping.others",
        "View another player's latency"
    )

    internal val NEAR: PermissionSpec = playerNode(
        "near",
        "List nearby players"
    )

    internal val NEAR_BYPASS_RADIUS: PermissionSpec = moderatorNode(
        "near.bypass_radius",
        "Exceed the configured /near radius"
    )

    internal val NICK: PermissionSpec = playerNode(
        "nick",
        "Set or clear your nickname"
    )

    internal val REALNAME: PermissionSpec = playerNode(
        "realname",
        "Resolve a nickname to a player name"
    )

    internal val NICK_OTHERS: PermissionSpec = moderatorNode(
        "nick.others",
        "Set or clear another player's nickname"
    )

    internal val PLAY_TIME_OTHERS: PermissionSpec = moderatorNode(
        "playtime.others",
        "View another player's play time"
    )

    internal val LIST: PermissionSpec = playerNode(
        "list",
        "List online players"
    )

    internal val LIST_HIDDEN: PermissionSpec = PermissionSpec(
        "cellulosesz.list.hidden",
        VanillaPermissionFallback.COMMANDS_MODERATOR,
        "See vanished players in the list"
    )

    internal val WHOIS_IP: PermissionSpec = PermissionSpec(
        "cellulosesz.whois.ip",
        VanillaPermissionFallback.COMMANDS_ADMIN,
        "See a player's IP address"
    )

    internal val AFK_OTHERS: PermissionSpec = PermissionSpec(
        "cellulosesz.afk.others",
        VanillaPermissionFallback.COMMANDS_MODERATOR,
        "Toggle another player's AFK state"
    )

    internal val AFK_BYPASS: PermissionSpec = PermissionSpec(
        "cellulosesz.afk.bypass",
        VanillaPermissionFallback.COMMANDS_MODERATOR,
        "Never be marked AFK automatically"
    )

    internal val NICK_COLORS: PermissionSpec = PermissionSpec(
        "cellulosesz.nick.colors",
        VanillaPermissionFallback.COMMANDS_MODERATOR,
        "Use colour codes in your nickname"
    )

}
