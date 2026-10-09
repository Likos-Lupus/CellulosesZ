package top.likoslupus.cellulosesz.core.permission

import net.minecraft.commands.CommandSourceStack
import net.minecraft.server.permissions.Permissions

/**
 * The vanilla capability a node falls back to when no third-party permission provider expresses an
 * opinion. Undefined must mean "vanilla", never "denied".
 */
public enum class VanillaPermissionFallback {

    ALLOW_ALL,
    COMMANDS_MODERATOR,
    COMMANDS_GAMEMASTER,
    COMMANDS_ADMIN,
    COMMANDS_OWNER,
    DENY_ALL,

}

/** A stable permission node plus the vanilla behaviour used when the node is not defined. */
public data class PermissionSpec(
    public val node: String,
    public val fallback: VanillaPermissionFallback,
    public val description: String,
)

/** Evaluates the vanilla fallback for [spec] against [source]. */
public fun vanillaFallback(
    source: CommandSourceStack,
    spec: PermissionSpec
): Boolean =
    when (spec.fallback) {
        VanillaPermissionFallback.ALLOW_ALL -> true

        VanillaPermissionFallback.COMMANDS_MODERATOR -> source.permissions()
                .hasPermission(Permissions.COMMANDS_MODERATOR)

        VanillaPermissionFallback.COMMANDS_GAMEMASTER -> source.permissions()
                .hasPermission(Permissions.COMMANDS_GAMEMASTER)

        VanillaPermissionFallback.COMMANDS_ADMIN -> source.permissions()
                .hasPermission(Permissions.COMMANDS_ADMIN)

        VanillaPermissionFallback.COMMANDS_OWNER -> source.permissions()
                .hasPermission(Permissions.COMMANDS_OWNER)

        VanillaPermissionFallback.DENY_ALL -> false
    }
