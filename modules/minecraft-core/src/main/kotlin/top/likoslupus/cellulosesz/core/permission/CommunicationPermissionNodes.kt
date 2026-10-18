package top.likoslupus.cellulosesz.core.permission

import top.likoslupus.cellulosesz.core.permission.PermissionNodeFactory.moderatorNode
import top.likoslupus.cellulosesz.core.permission.PermissionNodeFactory.playerNode

/** Communication command nodes, grouped by the bounded context that owns the commands. */
internal object CommunicationPermissionNodes {

    internal val MSG: PermissionSpec = playerNode(
        "msg",
        "Send a private message"
    )

    internal val REPLY: PermissionSpec = playerNode(
        "reply",
        "Reply to a private message"
    )

    internal val IGNORE: PermissionSpec = playerNode(
        "ignore",
        "Manage ignored players"
    )

    internal val MSG_TOGGLE: PermissionSpec = playerNode(
        "msgtoggle",
        "Toggle private messages"
    )

    internal val REPLY_TOGGLE: PermissionSpec = playerNode(
        "rtoggle",
        "Toggle replying to the last recipient"
    )

    internal val MAIL: PermissionSpec = playerNode(
        "mail",
        "Use mail"
    )

    internal val HELP_OP: PermissionSpec = playerNode(
        "helpop",
        "Request staff help"
    )

    internal val BROADCAST: PermissionSpec = moderatorNode(
        "broadcast",
        "Broadcast a message"
    )

    internal val BROADCAST_WORLD: PermissionSpec = moderatorNode(
        "broadcastworld",
        "Broadcast to a world"
    )

    internal val MOTD: PermissionSpec = playerNode(
        "motd",
        "View the message of the day"
    )

    internal val RULES: PermissionSpec = playerNode(
        "rules",
        "View the server rules"
    )

    internal val INFO: PermissionSpec = playerNode(
        "info",
        "View the server info page"
    )

    internal val CUSTOMTEXT: PermissionSpec = playerNode(
        "customtext",
        "View a custom server page"
    )

    internal val ME: PermissionSpec = playerNode(
        "me",
        "Send an action message"
    )

}
