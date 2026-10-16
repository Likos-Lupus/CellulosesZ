package top.likoslupus.cellulosesz.api.movement

import top.likoslupus.cellulosesz.api.ApiPosition
import top.likoslupus.cellulosesz.api.actor.ApiActor
import top.likoslupus.cellulosesz.api.common.ApiCallback
import top.likoslupus.cellulosesz.api.common.ApiRequestHandle
import java.time.Instant
import java.util.UUID

/**
 * A warp entry. [position] may be hidden from list queries when the viewer lacks coordinate-detail
 * permission; a `null` position means "not disclosed to this viewer", not "missing".
 */
public class WarpView(
    public val name: String,
    public val position: ApiPosition?,
    public val createdBy: String?,
    public val updatedAt: Instant?,
)

public sealed interface WarpListOutcome {

    public data class Listed(public val warps: List<WarpView>) : WarpListOutcome

    public data object NotAuthorized : WarpListOutcome

    public data object StorageUnavailable : WarpListOutcome

}

public sealed interface WarpLookupOutcome {

    public data class Found(public val warp: WarpView) : WarpLookupOutcome

    public data object NotFound : WarpLookupOutcome

    public data object InvalidName : WarpLookupOutcome

    public data object NotAuthorized : WarpLookupOutcome

    public data object StorageUnavailable : WarpLookupOutcome

}

public sealed interface WarpMutationOutcome {

    public data class Created(public val name: String) : WarpMutationOutcome

    public data class Updated(public val name: String) : WarpMutationOutcome

    public data class Deleted(public val name: String) : WarpMutationOutcome

    public data object NotFound : WarpMutationOutcome

    public data object InvalidName : WarpMutationOutcome

    public data object NotAuthorized : WarpMutationOutcome

    public data object PlayerOffline : WarpMutationOutcome

    public data object StorageUnavailable : WarpMutationOutcome

}

/**
 * Warp reads (filtered by the viewer's own visibility/permission) and privileged mutations. Mutations
 * carry the acting [ApiActor.Player] so the composition root can authorize and attribute the change.
 */
public interface WarpsApi {

    public fun listVisible(
        viewerId: UUID,
        callback: ApiCallback<WarpListOutcome>,
    ): ApiRequestHandle

    public fun findVisible(
        viewerId: UUID,
        name: String,
        callback: ApiCallback<WarpLookupOutcome>,
    ): ApiRequestHandle

    public fun create(
        actor: ApiActor.Player,
        name: String,
        callback: ApiCallback<WarpMutationOutcome>,
    ): ApiRequestHandle

    public fun delete(
        actor: ApiActor.Player,
        name: String,
        callback: ApiCallback<WarpMutationOutcome>,
    ): ApiRequestHandle

}
