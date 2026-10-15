package top.likoslupus.cellulosesz.movement.teleport

import top.likoslupus.cellulosesz.movement.TeleportEndpoint
import top.likoslupus.cellulosesz.movement.TeleportOrigin

/** Maps the internal cause onto the narrower origin exposed to compatibility hooks. */
internal fun TeleportCause.toOrigin(): TeleportOrigin =
    when (this) {
        TeleportCause.HOME -> TeleportOrigin.HOME
        TeleportCause.WARP -> TeleportOrigin.WARP
        TeleportCause.SPAWN -> TeleportOrigin.SPAWN
        TeleportCause.BACK -> TeleportOrigin.BACK
        TeleportCause.REQUEST -> TeleportOrigin.REQUEST
        TeleportCause.DIRECT -> TeleportOrigin.DIRECT
        TeleportCause.ADMIN -> TeleportOrigin.ADMIN

        TeleportCause.JAIL_CONFINEMENT,
        TeleportCause.JAIL_CORRECTION,
        TeleportCause.JAIL_RELEASE -> TeleportOrigin.JAIL

        TeleportCause.RANDOM -> TeleportOrigin.RANDOM

        TeleportCause.TOP,
        TeleportCause.BOTTOM,
        TeleportCause.JUMP -> TeleportOrigin.TERRAIN
    }

/** Projects a stored position to the Minecraft-free endpoint a hook may see. */
internal fun StoredPosition.toEndpoint(): TeleportEndpoint =
    TeleportEndpoint(
        dimension = dimension,
        x = x,
        y = y,
        z = z,
        yaw = yaw,
        pitch = pitch,
    )
