package top.likoslupus.cellulosesz.core.permission

import top.likoslupus.cellulosesz.core.permission.PermissionNodeFactory.playerNode

/** Root command nodes, grouped by the bounded context that owns the commands. */
internal object RootPermissionNodes {

    internal val ROOT: PermissionSpec = playerNode(
        "cellulosesz",
        "Use the CellulosesZ command"
    )

    internal val ROOT_LANGUAGE: PermissionSpec = PermissionSpec(
        "cellulosesz.command.cellulosesz.language",
        VanillaPermissionFallback.ALLOW_ALL,
        "Change your CellulosesZ language"
    )

    internal val ROOT_STATUS: PermissionSpec = PermissionSpec(
        "cellulosesz.command.cellulosesz.status",
        VanillaPermissionFallback.COMMANDS_MODERATOR,
        "View CellulosesZ status"
    )

    internal val ROOT_RELOAD: PermissionSpec = PermissionSpec(
        "cellulosesz.command.cellulosesz.reload",
        VanillaPermissionFallback.COMMANDS_MODERATOR,
        "Reload CellulosesZ configuration"
    )

    internal val ROOT_COMMANDS: PermissionSpec = PermissionSpec(
        "cellulosesz.command.cellulosesz.commands",
        VanillaPermissionFallback.COMMANDS_MODERATOR,
        "Inspect CellulosesZ command routes"
    )

    internal val ROOT_PERMISSIONS: PermissionSpec = PermissionSpec(
        "cellulosesz.command.cellulosesz.permissions",
        VanillaPermissionFallback.COMMANDS_MODERATOR,
        "Inspect CellulosesZ permission nodes"
    )

    /** Internal capability used by moderator notices and staff broadcasts (not a command node). */
    internal val MODERATOR: PermissionSpec = PermissionSpec(
        "cellulosesz.moderation.moderator",
        VanillaPermissionFallback.COMMANDS_MODERATOR,
        "CellulosesZ moderator capability"
    )

}
