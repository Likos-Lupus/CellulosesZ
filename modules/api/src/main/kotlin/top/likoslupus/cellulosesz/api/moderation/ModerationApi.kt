package top.likoslupus.cellulosesz.api.moderation

import top.likoslupus.cellulosesz.api.ApiQuery
import top.likoslupus.cellulosesz.api.actor.AddonAccess
import top.likoslupus.cellulosesz.api.common.ApiCallback
import top.likoslupus.cellulosesz.api.common.ApiRequestHandle
import java.time.Duration
import java.time.Instant
import java.util.*

/** A coarse moderation status summary. Detail beyond this requires the appropriate capability. */
public class ModerationStatusView(
    public val muted: Boolean,
    public val mutedUntil: Instant?,
    public val confined: Boolean,
)

public sealed interface ModerationStatusOutcome {

    public data class Known(public val summary: ModerationStatusView) : ModerationStatusOutcome

    public data object NotFound : ModerationStatusOutcome

    public data object NotAuthorized : ModerationStatusOutcome

    public data object StorageUnavailable : ModerationStatusOutcome

}

/** The actor of a moderation action: a player admin or an authorized addon. Never a free string. */
public sealed interface ModerationActionActor {

    public data class Player(public val playerId: UUID) : ModerationActionActor

    public data class Addon(public val access: AddonAccess) : ModerationActionActor

}

public class ModerationRequest(
    public val actor: ModerationActionActor,
    public val targetId: UUID,
    public val reason: String,
    public val duration: Duration?,
)

public class ModerationConfinementRequest(
    public val actor: ModerationActionActor,
    public val targetId: UUID,
    public val reason: String,
)

public sealed interface ModerationActionOutcome {

    public data class Applied(public val actionId: String) : ModerationActionOutcome

    public data object AlreadyInState : ModerationActionOutcome

    public data object TargetUnknown : ModerationActionOutcome

    public data object ProtectedTarget : ModerationActionOutcome

    public data object InvalidDuration : ModerationActionOutcome

    public data object NotAuthorized : ModerationActionOutcome

    public data object StorageUnavailable : ModerationActionOutcome

    /** The penalty applied but its audit record did not — surfaced, not silently swallowed. */
    public data object PartiallyApplied : ModerationActionOutcome

}

public interface ModerationStatusApi {

    public fun isMuted(viewerId: UUID, subjectId: UUID): ApiQuery<Boolean>

    public fun status(
        viewerId: UUID,
        subjectId: UUID,
        callback: ApiCallback<ModerationStatusOutcome>,
    ): ApiRequestHandle

}

/** Authorized moderation actions through the existing services, target protection and audit trail. */
public interface ModerationActionsApi {

    public fun mute(
        request: ModerationRequest,
        callback: ApiCallback<ModerationActionOutcome>,
    ): ApiRequestHandle

    public fun unmute(
        request: ModerationRequest,
        callback: ApiCallback<ModerationActionOutcome>,
    ): ApiRequestHandle

    public fun confine(
        request: ModerationConfinementRequest,
        callback: ApiCallback<ModerationActionOutcome>,
    ): ApiRequestHandle

    public fun release(
        request: ModerationConfinementRequest,
        callback: ApiCallback<ModerationActionOutcome>,
    ): ApiRequestHandle

}

public interface ModerationApi {

    public val status: ModerationStatusApi

    public val actions: ModerationActionsApi

    public val events: ModerationEvents

}
