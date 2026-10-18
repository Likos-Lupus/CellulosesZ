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
import top.likoslupus.cellulosesz.movement.home.HomeDataException
import top.likoslupus.cellulosesz.movement.home.HomeLookupResult
import top.likoslupus.cellulosesz.movement.home.HomeService
import top.likoslupus.cellulosesz.movement.location.LastLocationService
import top.likoslupus.cellulosesz.movement.request.TeleportRequestService
import top.likoslupus.cellulosesz.movement.request.TpAutoService
import top.likoslupus.cellulosesz.movement.spawn.SpawnDataException
import top.likoslupus.cellulosesz.movement.spawn.SpawnService
import top.likoslupus.cellulosesz.movement.teleport.StoredPosition
import top.likoslupus.cellulosesz.movement.teleport.TeleportBackend
import top.likoslupus.cellulosesz.movement.teleport.TeleportCause
import top.likoslupus.cellulosesz.movement.teleport.TeleportCoordinator
import top.likoslupus.cellulosesz.movement.teleport.TeleportDestination
import top.likoslupus.cellulosesz.movement.teleport.TeleportIntent
import top.likoslupus.cellulosesz.movement.teleport.teleportPolicyFor
import top.likoslupus.cellulosesz.movement.warp.WarpDataException
import top.likoslupus.cellulosesz.movement.warp.WarpLookupResult
import top.likoslupus.cellulosesz.movement.warp.WarpService
import java.util.UUID

/**
 * The production [MovementIntegrationPort]. It holds the live Movement services and resolves every
 * player/world fact on the server thread; it never calls the Minecraft teleport API directly.
 *
 * The port stays one implementation, but its cohesive areas are delegated to internal collaborators
 * ([MovementHomeIntegration], [MovementWarpIntegration], [MovementSpawnIntegration],
 * [MovementRequestIntegration]) that share one [MovementIntegrationContext]. Travel stays here
 * because it resolves destinations across several features and always commits through the single
 * teleport path.
 */
internal class DefaultMovementIntegration(
    teleports: TeleportCoordinator,
    private val homes: HomeService,
    private val warps: WarpService,
    private val spawn: SpawnService,
    requests: TeleportRequestService,
    tpAuto: TpAutoService,
    backend: TeleportBackend,
    lastLocations: LastLocationService,
    settings: () -> MovementSettings,
    kernel: RuntimeKernel,
    permissions: PermissionService,
    access: MovementAccess,
    observer: () -> MovementDomainObserver,
) : MovementIntegrationPort {

    private val context = MovementIntegrationContext(
        kernel = kernel,
        permissions = permissions,
        access = access,
        teleports = teleports,
        backend = backend,
        settings = settings,
        observer = observer,
    )

    private val homeIntegration = MovementHomeIntegration(homes, context)
    private val warpIntegration = MovementWarpIntegration(warps, context)
    private val spawnIntegration = MovementSpawnIntegration(spawn, lastLocations, context)
    private val requestIntegration = MovementRequestIntegration(requests, tpAuto, context)

    // -- Homes ----------------------------------------------------------------

    override suspend fun listHomes(ownerId: UUID): MovementHomeListResult =
        homeIntegration.listHomes(ownerId)

    override suspend fun findHome(ownerId: UUID, name: String?): MovementHomeLookupResult =
        homeIntegration.findHome(ownerId, name)

    override suspend fun setHomeAtCurrentPosition(
        actorId: UUID,
        name: String?,
    ): MovementHomeMutationResult =
        homeIntegration.setHomeAtCurrentPosition(actorId, name)

    override suspend fun renameHome(
        actorId: UUID,
        oldName: String,
        newName: String,
    ): MovementHomeMutationResult =
        homeIntegration.renameHome(actorId, oldName, newName)

    override suspend fun deleteHome(actorId: UUID, name: String): MovementHomeMutationResult =
        homeIntegration.deleteHome(actorId, name)

    // -- Warps ----------------------------------------------------------------

    override suspend fun listWarps(viewerId: UUID): MovementWarpListResult =
        warpIntegration.listWarps(viewerId)

    override suspend fun findWarp(viewerId: UUID, name: String): MovementWarpLookupResult =
        warpIntegration.findWarp(viewerId, name)

    override suspend fun createWarp(actorId: UUID, name: String): MovementWarpMutationResult =
        warpIntegration.createWarp(actorId, name)

    override suspend fun deleteWarp(actorId: UUID, name: String): MovementWarpMutationResult =
        warpIntegration.deleteWarp(actorId, name)

    // -- Spawn and last-known locations ---------------------------------------

    override suspend fun lastKnownLocation(playerId: UUID): MovementLastLocationResult =
        spawnIntegration.lastKnownLocation(playerId)

    override suspend fun configuredSpawn(viewerId: UUID): MovementSpawnQueryResult =
        spawnIntegration.configuredSpawn(viewerId)

    override suspend fun setSpawnFromCurrent(actorId: UUID): MovementSpawnMutationResult =
        spawnIntegration.setSpawnFromCurrent(actorId)

    override suspend fun clearSpawn(actorId: UUID): MovementSpawnMutationResult =
        spawnIntegration.clearSpawn(actorId)

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
        when (context.verdict(playerId, CommandPermissions.HOME)) {
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
        if (!context.access.allowsHomeInWorld(playerId, destination.dimension)) {
            return MovementTravelResult.Denied
        }
        return context.executeFixed(playerId, destination, TeleportCause.HOME)
    }

    private suspend fun travelToWarp(playerId: UUID, name: String): MovementTravelResult {
        when (context.verdict(playerId, CommandPermissions.WARP)) {
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
        if (!context.access.allowsWarp(playerId, name)) {
            return MovementTravelResult.Denied
        }
        return context.executeFixed(playerId, destination, TeleportCause.WARP)
    }

    private suspend fun travelToWorld(playerId: UUID, worldId: String): MovementTravelResult {
        when (context.verdict(playerId, CommandPermissions.WORLD)) {
            Verdict.DENIED -> return MovementTravelResult.Denied
            Verdict.OFFLINE -> return MovementTravelResult.PlayerOffline
            Verdict.ALLOWED -> Unit
        }
        if (!context.access.allows(playerId, worldId)) {
            return MovementTravelResult.Denied
        }
        val destination = context.kernel.onServerThread {
            val identifier = Identifier.tryParse(worldId)
                ?: return@onServerThread null
            val level = context.kernel.requireServer().getLevel(
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
        return context.executeFixed(playerId, destination, TeleportCause.SPAWN)
    }

    private suspend fun travelToSpawn(playerId: UUID): MovementTravelResult {
        when (context.verdict(playerId, CommandPermissions.SPAWN)) {
            Verdict.DENIED -> return MovementTravelResult.Denied
            Verdict.OFFLINE -> return MovementTravelResult.PlayerOffline
            Verdict.ALLOWED -> Unit
        }
        val destination = try {
            spawn.configured()
        } catch (_: SpawnDataException) {
            return MovementTravelResult.OtherFailure
        } ?: context.backend.vanillaSpawnPosition()
        return context.executeFixed(playerId, destination, TeleportCause.SPAWN)
    }

    private suspend fun travelToPlayer(playerId: UUID, targetId: UUID): MovementTravelResult {
        when (context.verdict(playerId, CommandPermissions.TP)) {
            Verdict.DENIED -> return MovementTravelResult.Denied
            Verdict.OFFLINE -> return MovementTravelResult.PlayerOffline
            Verdict.ALLOWED -> Unit
        }
        return context.executeToPlayer(playerId, targetId)
    }

    // -- Teleport requests ----------------------------------------------------

    override suspend fun sendTeleportRequest(
        playerId: UUID,
        targetId: UUID,
    ): MovementRequestActionResult =
        requestIntegration.sendTeleportRequest(playerId, targetId)

    override suspend fun acceptTeleportRequest(
        playerId: UUID,
        requestId: String,
    ): MovementRequestActionResult =
        requestIntegration.acceptTeleportRequest(playerId, requestId)

    override suspend fun denyTeleportRequest(
        playerId: UUID,
        requestId: String,
    ): MovementRequestActionResult =
        requestIntegration.denyTeleportRequest(playerId, requestId)

    override suspend fun cancelOwnTeleportRequest(
        playerId: UUID,
        requestId: String,
    ): MovementRequestActionResult =
        requestIntegration.cancelOwnTeleportRequest(playerId, requestId)

}

/**
 * State and server-thread helpers shared by the port implementation and its collaborators. Every
 * player/world fact is resolved on the server thread; nothing here caches a `ServerPlayer`.
 */
internal class MovementIntegrationContext(
    val kernel: RuntimeKernel,
    val permissions: PermissionService,
    val access: MovementAccess,
    val teleports: TeleportCoordinator,
    val backend: TeleportBackend,
    val settings: () -> MovementSettings,
    val observer: () -> MovementDomainObserver,
) {

    suspend fun online(playerId: UUID): Boolean =
        kernel.onServerThread {
            PlayerResolver.onlineById(
                kernel.requireServer(),
                playerId
            ) != null
        }

    suspend fun onlineName(playerId: UUID): String? =
        kernel.onServerThread {
            PlayerResolver.onlineById(
                kernel.requireServer(),
                playerId
            )?.name?.string
        }

    suspend fun verdict(playerId: UUID, spec: PermissionSpec): Verdict =
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

    suspend fun executeFixed(
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

    /** Commits a player-to-player move; the coordinator resolves the destination at commit time. */
    suspend fun executeToPlayer(
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

    suspend fun notifyHomeChanged(
        ownerId: UUID,
        name: String,
        kind: MovementHomeChangeKind,
    ) =
        kernel.onServerThread {
            observer().onHomeChanged(ownerId, name, kind)
        }

    suspend fun notifyWarpChanged(name: String, kind: MovementWarpChangeKind) =
        kernel.onServerThread {
            observer().onWarpChanged(name, kind)
        }

    suspend fun notifySpawnChanged(configured: Boolean) =
        kernel.onServerThread {
            observer().onSpawnChanged(configured)
        }

    suspend fun notifyRequestChanged(
        senderId: UUID,
        targetId: UUID,
        state: MovementRequestState,
    ) =
        kernel.onServerThread {
            observer().onTeleportRequestChanged(senderId, targetId, state)
        }

}

/** The authorization outcome for one player-scoped check. */
internal enum class Verdict {

    ALLOWED,
    DENIED,
    OFFLINE

}
