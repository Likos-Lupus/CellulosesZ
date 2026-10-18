package top.likoslupus.cellulosesz.api.movement

import top.likoslupus.cellulosesz.api.ApiPosition
import top.likoslupus.cellulosesz.api.ApiQuery
import top.likoslupus.cellulosesz.api.actor.AddonAccess
import top.likoslupus.cellulosesz.api.common.ApiCallback
import top.likoslupus.cellulosesz.api.common.ApiRequestHandle
import java.util.*

/**
 * A world (dimension) known to CellulosesZ. [id] is the dimension identifier; [alias] is the
 * configured display alias, or `null` when none is configured.
 */
public class WorldView(
    public val id: String,
    public val alias: String?,
)

public sealed interface WorldListOutcome {

    public data class Listed(public val worlds: List<WorldView>) : WorldListOutcome

    public data object NotAuthorized : WorldListOutcome

}

public sealed interface WorldLookupOutcome {

    public data class Found(public val world: WorldView) : WorldLookupOutcome

    public data object NotFound : WorldLookupOutcome

    public data object NotAuthorized : WorldLookupOutcome

}

public sealed interface WorldAccessOutcome {

    public data object Allowed : WorldAccessOutcome

    public data object Denied : WorldAccessOutcome

    public data object UnknownWorld : WorldAccessOutcome

    public data object NotAuthorized : WorldAccessOutcome

}

/** The last location CellulosesZ recorded for a player; [Found] carries the recorded position. */
public sealed interface LastLocationOutcome {

    public data class Found(public val position: ApiPosition) : LastLocationOutcome

    public data object Unknown : LastLocationOutcome

    public data object NotAuthorized : LastLocationOutcome

}

/**
 * World inventory and per-player world access. [list], [find] and [canAccess] are ungated reads;
 * [lastKnownLocation] is a privileged read and requires a capability on its [AddonAccess].
 */
public interface WorldsApi {

    public fun list(): ApiQuery<List<WorldView>>

    public fun find(worldId: String): ApiQuery<WorldView?>

    public fun canAccess(playerId: UUID, worldId: String): ApiQuery<Boolean>

    public fun lastKnownLocation(
        access: AddonAccess,
        playerId: UUID,
        callback: ApiCallback<LastLocationOutcome>,
    ): ApiRequestHandle

}
