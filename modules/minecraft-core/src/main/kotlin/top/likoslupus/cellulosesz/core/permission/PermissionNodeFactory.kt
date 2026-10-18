package top.likoslupus.cellulosesz.core.permission

/**
 * The one place that knows the `cellulosesz.command.` prefix and the three vanilla permission
 * fallbacks. Domain node objects call these factories; nothing else constructs a command node.
 */
internal object PermissionNodeFactory {

    internal const val PREFIX: String = "cellulosesz.command."

    internal fun playerNode(name: String, description: String): PermissionSpec =
        PermissionSpec(
            PREFIX + name,
            VanillaPermissionFallback.ALLOW_ALL,
            description,
        )

    internal fun moderatorNode(name: String, description: String): PermissionSpec =
        PermissionSpec(
            PREFIX + name,
            VanillaPermissionFallback.COMMANDS_MODERATOR,
            description,
        )

    internal fun adminNode(name: String, description: String): PermissionSpec =
        PermissionSpec(
            PREFIX + name,
            VanillaPermissionFallback.COMMANDS_ADMIN,
            description,
        )

}
