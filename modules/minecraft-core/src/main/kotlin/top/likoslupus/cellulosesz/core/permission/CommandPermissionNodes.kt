package top.likoslupus.cellulosesz.core.permission

/** Command command nodes, grouped by the bounded context that owns the commands. */
internal object CommandPermissionNodes {

    internal val COMMAND_COST_EXEMPT: PermissionSpec = PermissionSpec(
        "cellulosesz.command.cost.exempt",
        VanillaPermissionFallback.COMMANDS_MODERATOR,
        "Run commands without paying their configured cost"
    )

    internal val COMMAND_COOLDOWN_EXEMPT: PermissionSpec = PermissionSpec(
        "cellulosesz.command.cooldown.exempt",
        VanillaPermissionFallback.COMMANDS_MODERATOR,
        "Run commands without their configured cooldown"
    )

}
