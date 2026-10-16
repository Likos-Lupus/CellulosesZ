package top.likoslupus.cellulosesz.movement.integration

import top.likoslupus.cellulosesz.movement.TeleportOrigin
import java.time.Instant
import java.util.*
import kotlin.time.Duration

/**
 * Movement-local, Minecraft-free position passed across the integration boundary. It mirrors the
 * storage model but is deliberately independent of it so the application adapter never sees an
 * internal type.
 */
public class MovementPosition(
    public val dimension: String,
    public val x: Double,
    public val y: Double,
    public val z: Double,
    public val yaw: Float,
    public val pitch: Float,
)

/** A named home owned by [ownerId]. */
public class MovementHome(
    public val ownerId: UUID,
    public val name: String,
    public val position: MovementPosition,
)

public sealed interface MovementHomeListResult {

    public data class Listed(public val homes: List<MovementHome>) : MovementHomeListResult

    public data object StorageUnavailable : MovementHomeListResult

}

public sealed interface MovementHomeLookupResult {

    public data class Found(public val home: MovementHome) : MovementHomeLookupResult

    public data object NotFound : MovementHomeLookupResult

    public data object InvalidName : MovementHomeLookupResult

    public data object StorageUnavailable : MovementHomeLookupResult

}

public sealed interface MovementHomeMutationResult {

    public data class Created(public val name: String) : MovementHomeMutationResult

    public data class Updated(public val name: String) : MovementHomeMutationResult

    public data class Renamed(public val name: String) : MovementHomeMutationResult

    public data class Deleted(public val name: String) : MovementHomeMutationResult

    public data object InvalidName : MovementHomeMutationResult

    public data object NotFound : MovementHomeMutationResult

    public data object AlreadyExists : MovementHomeMutationResult

    public data object LimitReached : MovementHomeMutationResult

    public data object PlayerOffline : MovementHomeMutationResult

    public data object StorageUnavailable : MovementHomeMutationResult

}

/**
 * A warp entry. [position] is `null` when the coordinate-detail permission was not held by the
 * viewer, which is distinct from a missing warp.
 */
public class MovementWarp(
    public val name: String,
    public val position: MovementPosition?,
    public val createdBy: String?,
    public val updatedAt: Instant?,
)

public sealed interface MovementWarpListResult {

    public data class Listed(public val warps: List<MovementWarp>) : MovementWarpListResult

    public data object NotAuthorized : MovementWarpListResult

    public data object StorageUnavailable : MovementWarpListResult

}

public sealed interface MovementWarpLookupResult {

    public data class Found(public val warp: MovementWarp) : MovementWarpLookupResult

    public data object NotFound : MovementWarpLookupResult

    public data object InvalidName : MovementWarpLookupResult

    public data object NotAuthorized : MovementWarpLookupResult

    public data object StorageUnavailable : MovementWarpLookupResult

}

public sealed interface MovementWarpMutationResult {

    public data class Created(public val name: String) : MovementWarpMutationResult

    public data class Deleted(public val name: String) : MovementWarpMutationResult

    public data object NotFound : MovementWarpMutationResult

    public data object InvalidName : MovementWarpMutationResult

    public data object NotAuthorized : MovementWarpMutationResult

    public data object PlayerOffline : MovementWarpMutationResult

    public data object StorageUnavailable : MovementWarpMutationResult

}

public sealed interface MovementSpawnQueryResult {

    public data class Configured(public val position: MovementPosition) : MovementSpawnQueryResult

    public data object NotConfigured : MovementSpawnQueryResult

    public data object NotAuthorized : MovementSpawnQueryResult

    public data object StorageUnavailable : MovementSpawnQueryResult

}

public sealed interface MovementSpawnMutationResult {

    public data object Updated : MovementSpawnMutationResult

    public data object Cleared : MovementSpawnMutationResult

    public data object NotAuthorized : MovementSpawnMutationResult

    public data object PlayerOffline : MovementSpawnMutationResult

    public data object StorageUnavailable : MovementSpawnMutationResult

}

/** A destination the port resolves itself, then commits through the single teleport path. */
public sealed interface MovementTravelTarget {

    public data class Home(public val name: String?) : MovementTravelTarget

    public data class Warp(public val name: String) : MovementTravelTarget

    public data object Spawn : MovementTravelTarget

    public data class Player(public val playerId: UUID) : MovementTravelTarget

}

/** The committed receipt of a travel; [to] is the position the server actually applied. */
public class MovementTeleportReceipt(
    public val playerId: UUID,
    public val from: MovementPosition,
    public val to: MovementPosition,
    public val origin: TeleportOrigin,
)

public sealed interface MovementTravelResult {

    public data class Committed(public val receipt: MovementTeleportReceipt) : MovementTravelResult

    public data object PlayerOffline : MovementTravelResult

    public data object TargetOffline : MovementTravelResult

    public data object DestinationNotFound : MovementTravelResult

    public data object Denied : MovementTravelResult

    public data object Confined : MovementTravelResult

    public data object UnsafeDestination : MovementTravelResult

    public data object PlatformRejected : MovementTravelResult

    public data class Cooldown(public val remaining: Duration) : MovementTravelResult

    public data object AlreadyPending : MovementTravelResult

    public data object Cancelled : MovementTravelResult

    public data object OtherFailure : MovementTravelResult

}

public sealed interface MovementRequestActionResult {

    public data class Created(public val requestId: String) : MovementRequestActionResult

    public data object Accepted : MovementRequestActionResult

    public data object Denied : MovementRequestActionResult

    public data object Cancelled : MovementRequestActionResult

    public data object Expired : MovementRequestActionResult

    public data object TargetUnavailable : MovementRequestActionResult

    public data object AlreadyPending : MovementRequestActionResult

    public data object NotAuthorized : MovementRequestActionResult

    public data object RequestLimit : MovementRequestActionResult

    public data object OtherFailure : MovementRequestActionResult

}

public enum class MovementHomeChangeKind {

    CREATED,
    UPDATED,
    RENAMED,
    DELETED

}

public enum class MovementWarpChangeKind {

    CREATED,
    UPDATED,
    DELETED

}

public enum class MovementRequestState {

    PENDING,
    ACCEPTED,
    DENIED,
    CANCELLED,
    EXPIRED

}

/**
 * Committed movement-domain facts the composition root forwards to public listeners. Every callback
 * is best-effort and must never reverse an already-committed change.
 */
public interface MovementDomainObserver {

    public fun onHomeChanged(
        ownerId: UUID,
        name: String,
        kind: MovementHomeChangeKind
    )

    public fun onWarpChanged(name: String, kind: MovementWarpChangeKind)

    public fun onSpawnChanged(configured: Boolean)

    public fun onTeleportRequestChanged(
        senderId: UUID,
        targetId: UUID,
        state: MovementRequestState
    )

    public companion object {

        public val None: MovementDomainObserver = object : MovementDomainObserver {

            override fun onHomeChanged(
                ownerId: UUID,
                name: String,
                kind: MovementHomeChangeKind
            ) =
                Unit

            override fun onWarpChanged(name: String, kind: MovementWarpChangeKind) =
                Unit

            override fun onSpawnChanged(configured: Boolean) =
                Unit

            override fun onTeleportRequestChanged(
                senderId: UUID,
                targetId: UUID,
                state: MovementRequestState,
            ) =
                Unit

        }

    }

}

/**
 * Narrow integration surface for the Movement domain. It exposes only the operations the public API
 * needs and always commits movement through the single [top.likoslupus.cellulosesz.movement.teleport.TeleportCoordinator]
 * path, so confinement, safety, cooldown, history and preflight policy stay identical to the
 * commands. Only ordinary-player causes can be requested here; a caller can never select an
 * admin/jail/bypass cause nor override cooldown, history or policy.
 *
 * The permission policy mirrors the equivalent command node, evaluated against the acting player's
 * live source. Reads scoped to the caller's own homes are ungated; warp/spawn reads and all
 * mutations/travel use the same node the command uses.
 */
public interface MovementIntegrationPort {

    public suspend fun listHomes(ownerId: UUID): MovementHomeListResult

    public suspend fun findHome(ownerId: UUID, name: String?): MovementHomeLookupResult

    public suspend fun setHomeAtCurrentPosition(
        actorId: UUID,
        name: String?,
    ): MovementHomeMutationResult

    public suspend fun renameHome(
        actorId: UUID,
        oldName: String,
        newName: String,
    ): MovementHomeMutationResult

    public suspend fun deleteHome(actorId: UUID, name: String): MovementHomeMutationResult

    public suspend fun listWarps(viewerId: UUID): MovementWarpListResult

    public suspend fun findWarp(viewerId: UUID, name: String): MovementWarpLookupResult

    public suspend fun createWarp(actorId: UUID, name: String): MovementWarpMutationResult

    public suspend fun deleteWarp(actorId: UUID, name: String): MovementWarpMutationResult

    public suspend fun configuredSpawn(viewerId: UUID): MovementSpawnQueryResult

    public suspend fun setSpawnFromCurrent(actorId: UUID): MovementSpawnMutationResult

    public suspend fun clearSpawn(actorId: UUID): MovementSpawnMutationResult

    public suspend fun travel(playerId: UUID, target: MovementTravelTarget): MovementTravelResult

    public suspend fun sendTeleportRequest(
        playerId: UUID,
        targetId: UUID,
    ): MovementRequestActionResult

    public suspend fun acceptTeleportRequest(
        playerId: UUID,
        requestId: String,
    ): MovementRequestActionResult

    public suspend fun denyTeleportRequest(
        playerId: UUID,
        requestId: String,
    ): MovementRequestActionResult

    public suspend fun cancelOwnTeleportRequest(
        playerId: UUID,
        requestId: String,
    ): MovementRequestActionResult

}
