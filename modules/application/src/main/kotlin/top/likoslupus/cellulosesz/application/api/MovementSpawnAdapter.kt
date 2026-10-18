package top.likoslupus.cellulosesz.application.api

import top.likoslupus.cellulosesz.api.actor.ApiActor
import top.likoslupus.cellulosesz.api.common.ApiCallResult
import top.likoslupus.cellulosesz.api.common.ApiCallback
import top.likoslupus.cellulosesz.api.common.ApiRequestHandle
import top.likoslupus.cellulosesz.api.movement.*
import top.likoslupus.cellulosesz.movement.integration.*
import java.util.*

internal class MovementSpawnAdapter(
    private val port: MovementIntegrationPort,
    private val dispatcher: ApiRequestDispatcher,
) : SpawnApi {

    override fun configured(
        viewerId: UUID,
        callback: ApiCallback<SpawnQueryOutcome>,
    ): ApiRequestHandle = dispatcher.submit(
        task = {
            ApiCallResult.Completed(
                port.configuredSpawn(
                    viewerId
                ).toApi()
            )
        },
        deliver = callback::onComplete,
    )

    override fun travelToSpawn(
        playerId: UUID,
        callback: ApiCallback<TeleportActionOutcome>,
    ): ApiRequestHandle = dispatcher.submit(
        task = {
            ApiCallResult.Completed(
                port.travel(
                    playerId,
                    MovementTravelTarget.Spawn
                ).toApi()
            )
        },
        deliver = callback::onComplete,
    )

    override fun setFromCurrent(
        actor: ApiActor.Player,
        callback: ApiCallback<SpawnMutationOutcome>,
    ): ApiRequestHandle = dispatcher.submit(
        task = {
            ApiCallResult.Completed(
                port.setSpawnFromCurrent(
                    actor.playerId
                ).toApi()
            )
        },
        deliver = callback::onComplete,
    )

    override fun clear(
        actor: ApiActor.Player,
        callback: ApiCallback<SpawnMutationOutcome>,
    ): ApiRequestHandle = dispatcher.submit(
        task = {
            ApiCallResult.Completed(
                port.clearSpawn(
                    actor.playerId
                ).toApi()
            )
        },
        deliver = callback::onComplete,
    )

}
