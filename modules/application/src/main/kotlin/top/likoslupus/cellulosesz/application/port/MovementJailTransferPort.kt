package top.likoslupus.cellulosesz.application.port

import top.likoslupus.cellulosesz.administration.jail.JailDestination
import top.likoslupus.cellulosesz.administration.jail.JailTransferKind
import top.likoslupus.cellulosesz.administration.jail.JailTransferPort
import top.likoslupus.cellulosesz.administration.jail.JailTransferResult
import top.likoslupus.cellulosesz.movement.MovementFeature
import top.likoslupus.cellulosesz.movement.MovementTeleportCause
import top.likoslupus.cellulosesz.movement.MovementTransferResult
import java.util.*

/**
 * Adapts administration's jail transfer port onto movement's public transfer surface. The movement
 * facade is resolved lazily because administration is constructed before movement to break the
 * confinement/transfer wiring cycle.
 */
internal class MovementJailTransferPort(
    private val movement: () -> MovementFeature,
) : JailTransferPort {

    override suspend fun transfer(
        playerId: UUID,
        destination: JailDestination,
        kind: JailTransferKind,
    ): JailTransferResult =
        movement().teleportPlayerTo(
            playerId = playerId,
            dimension = destination.dimension,
            x = destination.x,
            y = destination.y,
            z = destination.z,
            yaw = destination.yaw,
            pitch = destination.pitch,
            cause = kind.toMovementCause(),
        ).toJailTransferResult()

    override suspend fun release(playerId: UUID): JailTransferResult =
        movement().teleportPlayerToSpawn(playerId, MovementTeleportCause.RELEASE)
                .toJailTransferResult()

}

private fun JailTransferKind.toMovementCause(): MovementTeleportCause =
    when (this) {
        JailTransferKind.CONFINEMENT -> MovementTeleportCause.CONFINEMENT
        JailTransferKind.CORRECTION -> MovementTeleportCause.CORRECTION
        JailTransferKind.RELEASE -> MovementTeleportCause.RELEASE
    }

private fun MovementTransferResult.toJailTransferResult(): JailTransferResult =
    when (this) {
        MovementTransferResult.Success -> JailTransferResult.Transferred
        MovementTransferResult.PlayerOffline -> JailTransferResult.PlayerOffline
        is MovementTransferResult.Failed -> JailTransferResult.Failed(reasonKey)
    }
