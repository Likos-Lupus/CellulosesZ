package top.likoslupus.cellulosesz.api.movement

import top.likoslupus.cellulosesz.api.ApiPosition
import top.likoslupus.cellulosesz.api.common.ApiCallback
import top.likoslupus.cellulosesz.api.common.ApiRequestHandle
import java.util.*

/** A named home owned by [ownerId]. Coordinates are only returned to an authorized viewer. */
public class HomeView(
    public val ownerId: UUID,
    public val name: String,
    public val position: ApiPosition,
)

public sealed interface HomeListOutcome {

    public data class Listed(public val homes: List<HomeView>) : HomeListOutcome

    public data object NotAuthorized : HomeListOutcome

    public data object StorageUnavailable : HomeListOutcome

}

public sealed interface HomeLookupOutcome {

    public data class Found(public val home: HomeView) : HomeLookupOutcome

    public data object NotFound : HomeLookupOutcome

    public data object InvalidName : HomeLookupOutcome

    public data object NotAuthorized : HomeLookupOutcome

    public data object StorageUnavailable : HomeLookupOutcome

}

public sealed interface HomeMutationOutcome {

    public data class Created(public val name: String) : HomeMutationOutcome

    public data class Updated(public val name: String) : HomeMutationOutcome

    public data class Renamed(public val name: String) : HomeMutationOutcome

    public data class Deleted(public val name: String) : HomeMutationOutcome

    public data object InvalidName : HomeMutationOutcome

    public data object NotFound : HomeMutationOutcome

    public data object AlreadyExists : HomeMutationOutcome

    public data object LimitReached : HomeMutationOutcome

    public data object NotAuthorized : HomeMutationOutcome

    public data object PlayerOffline : HomeMutationOutcome

    public data object StorageUnavailable : HomeMutationOutcome

}

/**
 * Home management for the requesting player. [setAtCurrentPosition] deliberately takes no coordinates:
 * a player may only register their own current location, so the gameplay rule cannot be bypassed by a
 * caller supplying arbitrary positions.
 */
public interface HomesApi {

    public fun listOwned(
        ownerId: UUID,
        callback: ApiCallback<HomeListOutcome>,
    ): ApiRequestHandle

    public fun findOwned(
        ownerId: UUID,
        name: String?,
        callback: ApiCallback<HomeLookupOutcome>,
    ): ApiRequestHandle

    public fun setAtCurrentPosition(
        actorId: UUID,
        name: String?,
        callback: ApiCallback<HomeMutationOutcome>,
    ): ApiRequestHandle

    public fun renameOwn(
        actorId: UUID,
        oldName: String,
        newName: String,
        callback: ApiCallback<HomeMutationOutcome>,
    ): ApiRequestHandle

    public fun deleteOwn(
        actorId: UUID,
        name: String,
        callback: ApiCallback<HomeMutationOutcome>,
    ): ApiRequestHandle

}
