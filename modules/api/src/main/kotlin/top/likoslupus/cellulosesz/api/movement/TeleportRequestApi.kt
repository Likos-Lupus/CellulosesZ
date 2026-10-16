package top.likoslupus.cellulosesz.api.movement

import top.likoslupus.cellulosesz.api.common.ApiCallback
import top.likoslupus.cellulosesz.api.common.ApiRequestHandle
import java.util.UUID

/** The outcome of a teleport-request (TPA) action. */
public sealed interface TeleportRequestActionOutcome {

    public data class Created(public val requestId: String) : TeleportRequestActionOutcome

    public data object Accepted : TeleportRequestActionOutcome

    public data object Denied : TeleportRequestActionOutcome

    public data object Cancelled : TeleportRequestActionOutcome

    public data object Expired : TeleportRequestActionOutcome

    public data object TargetUnavailable : TeleportRequestActionOutcome

    public data object AlreadyPending : TeleportRequestActionOutcome

    public data object NotAuthorized : TeleportRequestActionOutcome

    public data object RequestLimit : TeleportRequestActionOutcome

    public data object OtherFailure : TeleportRequestActionOutcome

}

/**
 * Teleport-request actions driven through the existing request service, so reachability, request
 * limits and server-thread scheduling are unchanged. All inputs are UUIDs; none may bypass the
 * sender/target visibility rules.
 */
public interface TeleportRequestApi {

    public fun send(
        playerId: UUID,
        targetId: UUID,
        callback: ApiCallback<TeleportRequestActionOutcome>,
    ): ApiRequestHandle

    public fun accept(
        playerId: UUID,
        requestId: String,
        callback: ApiCallback<TeleportRequestActionOutcome>,
    ): ApiRequestHandle

    public fun deny(
        playerId: UUID,
        requestId: String,
        callback: ApiCallback<TeleportRequestActionOutcome>,
    ): ApiRequestHandle

    public fun cancelOwn(
        playerId: UUID,
        requestId: String,
        callback: ApiCallback<TeleportRequestActionOutcome>,
    ): ApiRequestHandle

}
