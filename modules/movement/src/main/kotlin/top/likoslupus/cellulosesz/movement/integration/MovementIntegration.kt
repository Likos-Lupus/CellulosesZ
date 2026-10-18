package top.likoslupus.cellulosesz.movement.integration

import net.minecraft.core.registries.Registries
import net.minecraft.resources.Identifier
import net.minecraft.resources.ResourceKey
import top.likoslupus.cellulosesz.core.permission.CommandPermissions
import top.likoslupus.cellulosesz.core.permission.PermissionService
import top.likoslupus.cellulosesz.core.permission.PermissionSpec
import top.likoslupus.cellulosesz.core.player.PlayerResolver
import top.likoslupus.cellulosesz.core.runtime.RuntimeKernel
import top.likoslupus.cellulosesz.movement.access.MovementAccess
import top.likoslupus.cellulosesz.movement.config.MovementSettings
import top.likoslupus.cellulosesz.movement.home.*
import top.likoslupus.cellulosesz.movement.location.LastLocationService
import top.likoslupus.cellulosesz.movement.request.*
import top.likoslupus.cellulosesz.movement.spawn.ClearSpawnResult
import top.likoslupus.cellulosesz.movement.spawn.SpawnDataException
import top.likoslupus.cellulosesz.movement.spawn.SpawnService
import top.likoslupus.cellulosesz.movement.teleport.*
import top.likoslupus.cellulosesz.movement.warp.*
import java.time.Instant
import java.util.*

/**
 * The production [MovementIntegrationPort]. It holds the live Movement services and resolves every
 * player/world fact on the server thread; it never calls the Minecraft teleport API directly.
 */
internal class DefaultMovementIntegration(
    private val teleports: TeleportCoordinator,
    private val homes: HomeService,
    private val warps: WarpService,
    private val spawn: SpawnService,
    private val requests: TeleportRequestService,
    private val tpAuto: TpAutoService,
    private val backend: TeleportBackend,
    private val lastLocations: LastLocationService,
    private val settings: () -> MovementSettings,
    private val kernel: RuntimeKernel,
    private val permissions: PermissionService,
    private val access: MovementAccess,
    private val observer: () -> MovementDomainObserver,
) : MovementIntegrationPort {

    // -- Homes ----------------------------------------------------------------

    override suspend fun listHomes(ownerId: UUID): MovementHomeListResult =
        try {
            MovementHomeListResult.Listed(homes.list(ownerId).map { it.toMovement(ownerId) })
        } catch (_: HomeDataException) {
            MovementHomeListResult.StorageUnavailable
        }

    override suspend fun findHome(ownerId: UUID, name: String?): MovementHomeLookupResult =
        try {
            when (val result = homes.get(ownerId, name)) {
                is HomeLookupResult.Found ->
                    MovementHomeLookupResult.Found(result.home.toMovement(ownerId))

                HomeLookupResult.InvalidName ->
                    MovementHomeLookupResult.InvalidName

                HomeLookupResult.NotFound ->
                    MovementHomeLookupResult.NotFound
            }
        } catch (_: HomeDataException) {
            MovementHomeLookupResult.StorageUnavailable
        }

    override suspend fun setHomeAtCurrentPosition(
        actorId: UUID,
        name: String?,
    ): MovementHomeMutationResult {
        return try {
            val position = backend.position(actorId)
                ?: return MovementHomeMutationResult.PlayerOffline
            val resolvedName = name ?: settings().homes.defaultName
            val existed = homes.get(actorId, resolvedName) is HomeLookupResult.Found
            when (homes.set(actorId, name, position)) {
                SetHomeResult.Success -> {
                    if (existed) {
                        notifyHomeChanged(actorId, resolvedName, MovementHomeChangeKind.UPDATED)
                        MovementHomeMutationResult.Updated(resolvedName)
                    } else {
                        notifyHomeChanged(actorId, resolvedName, MovementHomeChangeKind.CREATED)
                        MovementHomeMutationResult.Created(resolvedName)
                    }
                }

                SetHomeResult.InvalidName -> MovementHomeMutationResult.InvalidName
                SetHomeResult.LimitReached -> MovementHomeMutationResult.LimitReached
            }
        } catch (_: HomeDataException) {
            MovementHomeMutationResult.StorageUnavailable
        }
    }

    override suspend fun renameHome(
        actorId: UUID,
        oldName: String,
        newName: String,
    ): MovementHomeMutationResult =
        try {
            when (homes.rename(actorId, oldName, newName)) {
                RenameHomeResult.Renamed -> {
                    notifyHomeChanged(actorId, newName, MovementHomeChangeKind.RENAMED)
                    MovementHomeMutationResult.Renamed(newName)
                }

                RenameHomeResult.InvalidName -> MovementHomeMutationResult.InvalidName
                RenameHomeResult.SourceNotFound -> MovementHomeMutationResult.NotFound
                RenameHomeResult.DestinationExists -> MovementHomeMutationResult.AlreadyExists
            }
        } catch (_: HomeDataException) {
            MovementHomeMutationResult.StorageUnavailable
        }

    override suspend fun deleteHome(actorId: UUID, name: String): MovementHomeMutationResult =
        try {
            when (homes.delete(actorId, name)) {
                DeleteHomeResult.Deleted -> {
                    notifyHomeChanged(actorId, name, MovementHomeChangeKind.DELETED)
                    MovementHomeMutationResult.Deleted(name)
                }

                DeleteHomeResult.InvalidName -> MovementHomeMutationResult.InvalidName
                DeleteHomeResult.NotFound -> MovementHomeMutationResult.NotFound
            }
        } catch (_: HomeDataException) {
            MovementHomeMutationResult.StorageUnavailable
        }

    // -- Warps ----------------------------------------------------------------

    override suspend fun listWarps(viewerId: UUID): MovementWarpListResult {
        if (verdict(viewerId, CommandPermissions.WARPS) != Verdict.ALLOWED) {
            return MovementWarpListResult.NotAuthorized
        }
        val showCoordinates = verdict(
            viewerId,
            CommandPermissions.WARP_INFO_COORDINATES
        ) == Verdict.ALLOWED
        return try {
            MovementWarpListResult.Listed(
                access.visibleWarps(viewerId, warps.list()).map {
                    it.toMovement(showCoordinates, null)
                }
            )
        } catch (_: WarpDataException) {
            MovementWarpListResult.StorageUnavailable
        }
    }

    override suspend fun findWarp(viewerId: UUID, name: String): MovementWarpLookupResult {
        if (
            verdict(
                viewerId,
                CommandPermissions.WARP
            ) != Verdict.ALLOWED
        ) {
            return MovementWarpLookupResult.NotAuthorized
        }

        val showCoordinates = verdict(
            viewerId,
            CommandPermissions.WARP_INFO_COORDINATES
        ) == Verdict.ALLOWED
        return try {
            when (val result = warps.info(name)) {
                is WarpInfoResult.Found ->
                    if (!access.allowsWarp(viewerId, result.warp.name.value)) {
                        MovementWarpLookupResult.NotAuthorized
                    } else {
                        MovementWarpLookupResult.Found(
                            result.warp.toMovement(
                                showCoordinates,
                                result.metadata
                            )
                        )
                    }

                WarpInfoResult.InvalidName ->
                    MovementWarpLookupResult.InvalidName

                WarpInfoResult.NotFound ->
                    MovementWarpLookupResult.NotFound
            }
        } catch (_: WarpDataException) {
            MovementWarpLookupResult.StorageUnavailable
        }
    }

    override suspend fun createWarp(actorId: UUID, name: String): MovementWarpMutationResult {
        when (verdict(actorId, CommandPermissions.SET_WARP)) {
            Verdict.DENIED -> return MovementWarpMutationResult.NotAuthorized
            Verdict.OFFLINE -> return MovementWarpMutationResult.PlayerOffline
            Verdict.ALLOWED -> Unit
        }
        val position = backend.position(actorId)
            ?: return MovementWarpMutationResult.PlayerOffline
        val actorName = onlineName(actorId)
            ?: return MovementWarpMutationResult.PlayerOffline
        return try {
            when (warps.set(name, position, actorName, Instant.now())) {
                SetWarpResult.Success -> {
                    notifyWarpChanged(name, MovementWarpChangeKind.CREATED)
                    MovementWarpMutationResult.Created(name)
                }

                SetWarpResult.InvalidName -> MovementWarpMutationResult.InvalidName
            }
        } catch (_: WarpDataException) {
            MovementWarpMutationResult.StorageUnavailable
        }
    }

    override suspend fun deleteWarp(actorId: UUID, name: String): MovementWarpMutationResult {
        when (verdict(actorId, CommandPermissions.DEL_WARP)) {
            Verdict.DENIED -> return MovementWarpMutationResult.NotAuthorized
            Verdict.OFFLINE -> return MovementWarpMutationResult.PlayerOffline
            Verdict.ALLOWED -> Unit
        }
        return try {
            when (warps.delete(name)) {
                DeleteWarpResult.Deleted -> {
                    notifyWarpChanged(name, MovementWarpChangeKind.DELETED)
                    MovementWarpMutationResult.Deleted(name)
                }

                DeleteWarpResult.InvalidName -> MovementWarpMutationResult.InvalidName
                DeleteWarpResult.NotFound -> MovementWarpMutationResult.NotFound
            }
        } catch (_: WarpDataException) {
            MovementWarpMutationResult.StorageUnavailable
        }
    }

    override suspend fun lastKnownLocation(playerId: UUID): MovementLastLocationResult =
        when (val cached = lastLocations.lastLocation(playerId)) {
            null -> MovementLastLocationResult.Unknown
            else -> MovementLastLocationResult.Found(cached.position.toMovement())
        }

    // -- Spawn ----------------------------------------------------------------

    override suspend fun configuredSpawn(viewerId: UUID): MovementSpawnQueryResult {
        if (
            verdict(
                viewerId,
                CommandPermissions.SPAWN
            ) != Verdict.ALLOWED
        ) {
            return MovementSpawnQueryResult.NotAuthorized
        }
        return try {
            spawn.configured()?.let {
                MovementSpawnQueryResult.Configured(it.toMovement())
            } ?: MovementSpawnQueryResult.NotConfigured
        } catch (_: SpawnDataException) {
            MovementSpawnQueryResult.StorageUnavailable
        }
    }

    override suspend fun setSpawnFromCurrent(actorId: UUID): MovementSpawnMutationResult {
        when (verdict(actorId, CommandPermissions.SET_SPAWN)) {
            Verdict.DENIED -> return MovementSpawnMutationResult.NotAuthorized
            Verdict.OFFLINE -> return MovementSpawnMutationResult.PlayerOffline
            Verdict.ALLOWED -> Unit
        }
        val position = backend.position(actorId)
            ?: return MovementSpawnMutationResult.PlayerOffline
        return try {
            spawn.set(position)
            notifySpawnChanged(true)
            MovementSpawnMutationResult.Updated
        } catch (_: SpawnDataException) {
            MovementSpawnMutationResult.StorageUnavailable
        }
    }

    override suspend fun clearSpawn(actorId: UUID): MovementSpawnMutationResult {
        when (verdict(actorId, CommandPermissions.DEL_SPAWN)) {
            Verdict.DENIED -> return MovementSpawnMutationResult.NotAuthorized
            Verdict.OFFLINE -> return MovementSpawnMutationResult.PlayerOffline
            Verdict.ALLOWED -> Unit
        }
        return try {
            when (spawn.clear()) {
                ClearSpawnResult.Cleared -> {
                    notifySpawnChanged(false)
                    MovementSpawnMutationResult.Cleared
                }

                // Nothing was configured: the end state ("no spawn") already holds, and no change is
                // observed. The public contract has no distinct "not configured" branch.
                ClearSpawnResult.NotConfigured -> MovementSpawnMutationResult.Cleared
            }
        } catch (_: SpawnDataException) {
            MovementSpawnMutationResult.StorageUnavailable
        }
    }

    // -- Travel ---------------------------------------------------------------

    override suspend fun travel(
        playerId: UUID,
        target: MovementTravelTarget,
    ): MovementTravelResult =
        when (target) {
            is MovementTravelTarget.Home -> travelToHome(playerId, target.name)
            is MovementTravelTarget.Warp -> travelToWarp(playerId, target.name)
            MovementTravelTarget.Spawn -> travelToSpawn(playerId)
            is MovementTravelTarget.Player -> travelToPlayer(playerId, target.playerId)
            is MovementTravelTarget.World -> travelToWorld(playerId, target.worldId)
        }

    private suspend fun travelToHome(playerId: UUID, name: String?): MovementTravelResult {
        when (verdict(playerId, CommandPermissions.HOME)) {
            Verdict.DENIED -> return MovementTravelResult.Denied
            Verdict.OFFLINE -> return MovementTravelResult.PlayerOffline
            Verdict.ALLOWED -> Unit
        }
        val destination = try {
            when (val result = homes.get(playerId, name)) {
                is HomeLookupResult.Found -> result.home.position
                HomeLookupResult.InvalidName,
                HomeLookupResult.NotFound -> return MovementTravelResult.DestinationNotFound
            }
        } catch (_: HomeDataException) {
            return MovementTravelResult.OtherFailure
        }
        if (!access.allowsHomeInWorld(playerId, destination.dimension)) {
            return MovementTravelResult.Denied
        }
        return executeFixed(playerId, destination, TeleportCause.HOME)
    }

    private suspend fun travelToWarp(playerId: UUID, name: String): MovementTravelResult {
        when (verdict(playerId, CommandPermissions.WARP)) {
            Verdict.DENIED -> return MovementTravelResult.Denied
            Verdict.OFFLINE -> return MovementTravelResult.PlayerOffline
            Verdict.ALLOWED -> Unit
        }
        val destination = try {
            when (val result = warps.get(name)) {
                is WarpLookupResult.Found -> result.warp.position

                WarpLookupResult.InvalidName,
                WarpLookupResult.NotFound -> return MovementTravelResult.DestinationNotFound
            }
        } catch (_: WarpDataException) {
            return MovementTravelResult.OtherFailure
        }
        if (!access.allowsWarp(playerId, name)) {
            return MovementTravelResult.Denied
        }
        return executeFixed(playerId, destination, TeleportCause.WARP)
    }

    private suspend fun travelToWorld(playerId: UUID, worldId: String): MovementTravelResult {
        when (verdict(playerId, CommandPermissions.WORLD)) {
            Verdict.DENIED -> return MovementTravelResult.Denied
            Verdict.OFFLINE -> return MovementTravelResult.PlayerOffline
            Verdict.ALLOWED -> Unit
        }
        if (!access.allows(playerId, worldId)) {
            return MovementTravelResult.Denied
        }
        val destination = kernel.onServerThread {
            val identifier = Identifier.tryParse(worldId)
                ?: return@onServerThread null
            val level = kernel.requireServer().getLevel(
                ResourceKey.create(Registries.DIMENSION, identifier)
            ) ?: return@onServerThread null
            val spawn = level.respawnData
            StoredPosition(
                dimension = spawn.dimension().identifier().toString(),
                x = spawn.pos().x + 0.5,
                y = spawn.pos().y.toDouble(),
                z = spawn.pos().z + 0.5,
                yaw = spawn.yaw(),
                pitch = spawn.pitch(),
            )
        } ?: return MovementTravelResult.DestinationNotFound
        return executeFixed(playerId, destination, TeleportCause.SPAWN)
    }

    private suspend fun travelToSpawn(playerId: UUID): MovementTravelResult {
        when (verdict(playerId, CommandPermissions.SPAWN)) {
            Verdict.DENIED -> return MovementTravelResult.Denied
            Verdict.OFFLINE -> return MovementTravelResult.PlayerOffline
            Verdict.ALLOWED -> Unit
        }
        val destination = try {
            spawn.configured()
        } catch (_: SpawnDataException) {
            return MovementTravelResult.OtherFailure
        } ?: backend.vanillaSpawnPosition()
        return executeFixed(playerId, destination, TeleportCause.SPAWN)
    }

    private suspend fun travelToPlayer(playerId: UUID, targetId: UUID): MovementTravelResult {
        when (verdict(playerId, CommandPermissions.TP)) {
            Verdict.DENIED -> return MovementTravelResult.Denied
            Verdict.OFFLINE -> return MovementTravelResult.PlayerOffline
            Verdict.ALLOWED -> Unit
        }
        return teleports.execute(
            TeleportIntent(
                subjectId = playerId,
                // Resolved by the coordinator at commit time, never from a stale snapshot.
                destination = TeleportDestination.Player(targetId),
                cause = TeleportCause.REQUEST,
                policy = teleportPolicyFor(TeleportCause.REQUEST, settings().teleport),
            )
        ).toMovementTravelResult()
    }

    private suspend fun executeFixed(
        playerId: UUID,
        destination: StoredPosition,
        cause: TeleportCause,
    ): MovementTravelResult =
        teleports.execute(
            TeleportIntent(
                subjectId = playerId,
                destination = TeleportDestination.Fixed(destination),
                cause = cause,
                policy = teleportPolicyFor(cause, settings().teleport),
            )
        ).toMovementTravelResult()

    // -- Teleport requests ----------------------------------------------------

    override suspend fun sendTeleportRequest(
        playerId: UUID,
        targetId: UUID,
    ): MovementRequestActionResult {
        when (verdict(playerId, CommandPermissions.TPA)) {
            Verdict.DENIED,
            Verdict.OFFLINE -> return MovementRequestActionResult.NotAuthorized

            Verdict.ALLOWED -> Unit
        }
        return when {
            playerId == targetId -> MovementRequestActionResult.OtherFailure
            !online(targetId) -> MovementRequestActionResult.TargetUnavailable
            else -> when (
                kernel.onServerThread {
                    requests.send(
                        playerId,
                        targetId,
                        TeleportRequestType.TO_TARGET
                    )
                }
            ) {
                is TpaSendResult.Sent -> {
                    notifyRequestChanged(playerId, targetId, MovementRequestState.PENDING)
                    val autoAccepted = kernel.onServerThread {
                        tpAuto.shouldAutoAccept(
                            targetId,
                            playerId,
                            TeleportRequestType.TO_TARGET
                        ) && requests.accept(
                            targetId,
                            playerId
                        ) is TpaAcceptResult.Accepted
                    }
                    if (autoAccepted) {
                        notifyRequestChanged(
                            playerId,
                            targetId,
                            MovementRequestState.ACCEPTED
                        )
                        executeRequestTeleport(
                            playerId,
                            targetId
                        )
                        MovementRequestActionResult.Accepted
                    } else {
                        MovementRequestActionResult.Created(
                            requestId(
                                playerId,
                                targetId
                            )
                        )
                    }
                }

                is TpaSendResult.Refreshed ->
                    MovementRequestActionResult.AlreadyPending

                TpaSendResult.Self ->
                    MovementRequestActionResult.OtherFailure

                TpaSendResult.SenderQueueFull,
                TpaSendResult.ServerQueueFull,
                is TpaSendResult.TargetQueueFull ->
                    MovementRequestActionResult.RequestLimit

                is TpaSendResult.TargetDisabled ->
                    MovementRequestActionResult.TargetUnavailable
            }
        }
    }

    override suspend fun acceptTeleportRequest(
        playerId: UUID,
        requestId: String,
    ): MovementRequestActionResult {
        val parsed = parseRequestId(requestId)
            ?: return MovementRequestActionResult.NotAuthorized
        val (senderId, targetId) = parsed
        if (targetId != playerId) {
            return MovementRequestActionResult.NotAuthorized
        }
        when (verdict(playerId, CommandPermissions.TP_ACCEPT)) {
            Verdict.DENIED,
            Verdict.OFFLINE -> return MovementRequestActionResult.NotAuthorized

            Verdict.ALLOWED -> Unit
        }

        return when (
            val accepted = kernel.onServerThread {
                requests.accept(playerId, senderId)
            }
        ) {
            is TpaAcceptResult.Accepted -> {
                notifyRequestChanged(senderId, targetId, MovementRequestState.ACCEPTED)
                val request = accepted.request
                val subjectId = when (request.type) {
                    TeleportRequestType.TO_TARGET -> request.senderId
                    TeleportRequestType.BRING_TARGET -> request.targetId
                }
                val destinationId = when (subjectId) {
                    request.senderId -> request.targetId
                    else -> request.senderId
                }
                executeRequestTeleport(subjectId, destinationId)
                MovementRequestActionResult.Accepted
            }

            TpaAcceptResult.None -> {
                notifyRequestChanged(
                    senderId,
                    targetId,
                    MovementRequestState.EXPIRED
                )
                MovementRequestActionResult.Expired
            }

            TpaAcceptResult.Ambiguous ->
                MovementRequestActionResult.OtherFailure
        }
    }

    override suspend fun denyTeleportRequest(
        playerId: UUID,
        requestId: String,
    ): MovementRequestActionResult {
        val parsed = parseRequestId(requestId)
            ?: return MovementRequestActionResult.NotAuthorized
        val (senderId, targetId) = parsed
        if (targetId != playerId) {
            return MovementRequestActionResult.NotAuthorized
        }
        when (verdict(playerId, CommandPermissions.TP_DENY)) {
            Verdict.DENIED,
            Verdict.OFFLINE -> return MovementRequestActionResult.NotAuthorized

            Verdict.ALLOWED -> Unit
        }

        return when (
            kernel.onServerThread {
                requests.deny(playerId, senderId)
            }
        ) {
            is TpaDenyResult.Denied -> {
                notifyRequestChanged(
                    senderId,
                    targetId,
                    MovementRequestState.DENIED
                )
                MovementRequestActionResult.Denied
            }

            TpaDenyResult.None -> {
                notifyRequestChanged(
                    senderId,
                    targetId,
                    MovementRequestState.EXPIRED
                )
                MovementRequestActionResult.Expired
            }

            TpaDenyResult.Ambiguous ->
                MovementRequestActionResult.OtherFailure
        }
    }

    override suspend fun cancelOwnTeleportRequest(
        playerId: UUID,
        requestId: String,
    ): MovementRequestActionResult {
        val parsed = parseRequestId(requestId)
            ?: return MovementRequestActionResult.NotAuthorized
        val (senderId, targetId) = parsed
        if (senderId != playerId) {
            return MovementRequestActionResult.NotAuthorized
        }
        when (verdict(playerId, CommandPermissions.TP_CANCEL)) {
            Verdict.DENIED,
            Verdict.OFFLINE -> return MovementRequestActionResult.NotAuthorized

            Verdict.ALLOWED -> Unit
        }

        val removed = kernel.onServerThread {
            requests.cancelOne(playerId, targetId)
        }
        return when {
            !removed -> {
                notifyRequestChanged(
                    senderId,
                    targetId,
                    MovementRequestState.EXPIRED
                )
                MovementRequestActionResult.Expired
            }

            else -> {
                notifyRequestChanged(
                    senderId,
                    targetId,
                    MovementRequestState.CANCELLED
                )
                MovementRequestActionResult.Cancelled
            }
        }
    }

    private suspend fun executeRequestTeleport(
        subjectId: UUID,
        destinationId: UUID,
    ): MovementTravelResult =
        teleports.execute(
            TeleportIntent(
                subjectId = subjectId,
                destination = TeleportDestination.Player(destinationId),
                cause = TeleportCause.REQUEST,
                policy = teleportPolicyFor(
                    TeleportCause.REQUEST,
                    settings().teleport
                ),
            )
        ).toMovementTravelResult()

    // -- Server-thread and observation helpers --------------------------------

    private suspend fun online(playerId: UUID): Boolean =
        kernel.onServerThread {
            PlayerResolver.onlineById(
                kernel.requireServer(),
                playerId
            ) != null
        }

    private suspend fun onlineName(playerId: UUID): String? =
        kernel.onServerThread {
            PlayerResolver.onlineById(
                kernel.requireServer(),
                playerId
            )?.name?.string
        }

    private suspend fun verdict(playerId: UUID, spec: PermissionSpec): Verdict =
        kernel.onServerThread {
            val player = PlayerResolver.onlineById(
                kernel.requireServer(),
                playerId
            ) ?: return@onServerThread Verdict.OFFLINE
            when {
                permissions.allows(player.createCommandSourceStack(), spec) -> Verdict.ALLOWED
                else -> Verdict.DENIED
            }
        }

    private suspend fun notifyHomeChanged(
        ownerId: UUID,
        name: String,
        kind: MovementHomeChangeKind,
    ) =
        kernel.onServerThread {
            observer().onHomeChanged(ownerId, name, kind)
        }

    private suspend fun notifyWarpChanged(name: String, kind: MovementWarpChangeKind) =
        kernel.onServerThread {
            observer().onWarpChanged(name, kind)
        }

    private suspend fun notifySpawnChanged(configured: Boolean) =
        kernel.onServerThread {
            observer().onSpawnChanged(configured)
        }

    private suspend fun notifyRequestChanged(
        senderId: UUID,
        targetId: UUID,
        state: MovementRequestState,
    ) =
        kernel.onServerThread {
            observer().onTeleportRequestChanged(senderId, targetId, state)
        }

}

/** The authorization outcome for one player-scoped check. */
private enum class Verdict {

    ALLOWED,
    DENIED,
    OFFLINE

}

private fun requestId(senderId: UUID, targetId: UUID): String =
    "$senderId:$targetId"

private fun parseRequestId(raw: String): Pair<UUID, UUID>? {
    val separator = raw.indexOf(':')
    if (separator <= 0 || separator == raw.length - 1) {
        return null
    }
    val sender = runCatching {
        UUID.fromString(raw.substring(0, separator))
    }.getOrNull() ?: return null
    val target = runCatching {
        UUID.fromString(raw.substring(separator + 1))
    }.getOrNull() ?: return null
    return sender to target
}

private fun StoredPosition.toMovement(): MovementPosition =
    MovementPosition(dimension, x, y, z, yaw, pitch)

private fun Home.toMovement(ownerId: UUID): MovementHome =
    MovementHome(ownerId, name.value, position.toMovement())

private fun Warp.toMovement(showCoordinates: Boolean, metadata: WarpMetadata?): MovementWarp =
    MovementWarp(
        name = name.value,
        position = if (showCoordinates) position.toMovement() else null,
        createdBy = metadata?.createdBy,
        updatedAt = metadata?.updatedAt,
    )

private fun TeleportOutcome.toMovementTravelResult(): MovementTravelResult =
    when (this) {
        is TeleportOutcome.Success -> MovementTravelResult.Committed(
            MovementTeleportReceipt(
                playerId = receipt.subjectId,
                from = receipt.origin.toMovement(),
                to = receipt.destination.toMovement(),
                origin = receipt.cause.toOrigin(),
            )
        )

        TeleportOutcome.PlayerOffline -> MovementTravelResult.PlayerOffline
        TeleportOutcome.TargetOffline -> MovementTravelResult.TargetOffline
        // A stored dimension that no longer exists is an internal inconsistency, not a resolvable
        // destination miss: fold it into the generic failure.
        is TeleportOutcome.UnknownDimension -> MovementTravelResult.OtherFailure
        is TeleportOutcome.WorldAccessDenied -> MovementTravelResult.Denied
        TeleportOutcome.OutsideWorldBorder -> MovementTravelResult.OtherFailure
        TeleportOutcome.UnsafeDestination -> MovementTravelResult.UnsafeDestination
        is TeleportOutcome.Cooldown -> MovementTravelResult.Cooldown(remaining)
        TeleportOutcome.AlreadyPending -> MovementTravelResult.AlreadyPending
        TeleportOutcome.Confined -> MovementTravelResult.Confined
        is TeleportOutcome.Cancelled -> MovementTravelResult.Cancelled
        TeleportOutcome.PassengerConflict -> MovementTravelResult.OtherFailure
        TeleportOutcome.RejectedByPlatform -> MovementTravelResult.PlatformRejected
        is TeleportOutcome.RejectedByHook -> MovementTravelResult.Denied
        TeleportOutcome.RuntimeStopping -> MovementTravelResult.OtherFailure
    }
