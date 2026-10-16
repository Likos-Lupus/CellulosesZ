package top.likoslupus.cellulosesz.api.movement

import top.likoslupus.cellulosesz.api.ApiPosition
import top.likoslupus.cellulosesz.api.actor.ApiActor
import top.likoslupus.cellulosesz.api.common.ApiCallback
import top.likoslupus.cellulosesz.api.common.ApiRequestHandle
import java.util.*

/** Query result for the configured spawn. [NotConfigured] is separate from "the real spawn exists". */
public sealed interface SpawnQueryOutcome {

    public data class Configured(public val position: ApiPosition) : SpawnQueryOutcome

    public data object NotConfigured : SpawnQueryOutcome

    public data object NotAuthorized : SpawnQueryOutcome

    public data object StorageUnavailable : SpawnQueryOutcome

}

public sealed interface SpawnMutationOutcome {

    public data object Updated : SpawnMutationOutcome

    public data object Cleared : SpawnMutationOutcome

    public data object NotAuthorized : SpawnMutationOutcome

    public data object PlayerOffline : SpawnMutationOutcome

    public data object StorageUnavailable : SpawnMutationOutcome

}

/**
 * Spawn query/travel plus privileged configuration. [travelToSpawn] resolves the effective spawn (the
 * configured one, or the vanilla fallback) through the Coordinator, so it may differ from the
 * configuration query.
 */
public interface SpawnApi {

    public fun configured(
        viewerId: UUID,
        callback: ApiCallback<SpawnQueryOutcome>,
    ): ApiRequestHandle

    public fun travelToSpawn(
        playerId: UUID,
        callback: ApiCallback<TeleportActionOutcome>,
    ): ApiRequestHandle

    public fun setFromCurrent(
        actor: ApiActor.Player,
        callback: ApiCallback<SpawnMutationOutcome>,
    ): ApiRequestHandle

    public fun clear(
        actor: ApiActor.Player,
        callback: ApiCallback<SpawnMutationOutcome>,
    ): ApiRequestHandle

}
