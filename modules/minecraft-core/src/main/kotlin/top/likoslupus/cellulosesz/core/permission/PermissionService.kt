package top.likoslupus.cellulosesz.core.permission

import net.minecraft.commands.CommandSourceStack

/** The single authorization seam. Commands call `source.hasPermission(permissions, spec)`. */
public class PermissionService(
    public val bridge: PermissionBridge,
) {

    public fun allows(
        source: CommandSourceStack,
        spec: PermissionSpec
    ): Boolean =
        bridge.test(source, spec)

}

public fun CommandSourceStack.hasPermission(
    service: PermissionService,
    spec: PermissionSpec
): Boolean =
    service.allows(this, spec)
