package top.likoslupus.cellulosesz.core.command

import net.minecraft.commands.CommandSourceStack
import top.likoslupus.cellulosesz.core.permission.PermissionService
import top.likoslupus.cellulosesz.core.permission.PermissionSpec
import top.likoslupus.cellulosesz.core.permission.hasPermission

/**
 * The single command-facing authorization entry point. Command files never reference a loader or
 * third-party permission API directly; they ask the [PermissionService] for a [PermissionSpec].
 */
public fun CommandSourceStack.requiresPermission(
    permissions: PermissionService,
    spec: PermissionSpec,
): Boolean = hasPermission(permissions, spec)
