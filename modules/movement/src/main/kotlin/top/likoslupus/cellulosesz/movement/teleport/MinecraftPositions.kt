package top.likoslupus.cellulosesz.movement.teleport

import net.minecraft.world.entity.player.Player

/** Snapshot of an online player's position. Must be called on the server thread. */
internal fun Player.toStoredPosition(): StoredPosition =
    StoredPosition(
        dimension = level().dimension().identifier().toString(),
        x = x,
        y = y,
        z = z,
        yaw = yRot,
        pitch = xRot,
    )
