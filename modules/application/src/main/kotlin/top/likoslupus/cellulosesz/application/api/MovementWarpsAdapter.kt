package top.likoslupus.cellulosesz.application.api

import top.likoslupus.cellulosesz.api.actor.ApiActor
import top.likoslupus.cellulosesz.api.common.ApiCallResult
import top.likoslupus.cellulosesz.api.common.ApiCallback
import top.likoslupus.cellulosesz.api.common.ApiRequestHandle
import top.likoslupus.cellulosesz.api.movement.*
import top.likoslupus.cellulosesz.movement.integration.*
import java.util.*

internal class MovementWarpsAdapter(
    private val port: MovementIntegrationPort,
    private val dispatcher: ApiRequestDispatcher,
) : WarpsApi {

    override fun listVisible(
        viewerId: UUID,
        callback: ApiCallback<WarpListOutcome>,
    ): ApiRequestHandle = dispatcher.submit(
        task = {
            ApiCallResult.Completed(
                port.listWarps(
                    viewerId
                ).toApi()
            )
        },
        deliver = callback::onComplete,
    )

    override fun findVisible(
        viewerId: UUID,
        name: String,
        callback: ApiCallback<WarpLookupOutcome>,
    ): ApiRequestHandle = dispatcher.submit(
        task = {
            ApiCallResult.Completed(
                port.findWarp(
                    viewerId,
                    name
                ).toApi()
            )
        },
        deliver = callback::onComplete,
    )

    override fun create(
        actor: ApiActor.Player,
        name: String,
        callback: ApiCallback<WarpMutationOutcome>,
    ): ApiRequestHandle = dispatcher.submit(
        task = {
            ApiCallResult.Completed(
                port.createWarp(
                    actor.playerId,
                    name
                ).toApi()
            )
        },
        deliver = callback::onComplete,
    )

    override fun delete(
        actor: ApiActor.Player,
        name: String,
        callback: ApiCallback<WarpMutationOutcome>,
    ): ApiRequestHandle = dispatcher.submit(
        task = {
            ApiCallResult.Completed(
                port.deleteWarp(
                    actor.playerId,
                    name
                ).toApi()
            )
        },
        deliver = callback::onComplete,
    )

}
