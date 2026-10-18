package top.likoslupus.cellulosesz.application.api

import top.likoslupus.cellulosesz.api.common.ApiCallResult
import top.likoslupus.cellulosesz.api.common.ApiCallback
import top.likoslupus.cellulosesz.api.common.ApiRequestHandle
import top.likoslupus.cellulosesz.api.movement.*
import top.likoslupus.cellulosesz.movement.integration.*

internal class MovementTeleportAdapter(
    private val port: MovementIntegrationPort,
    private val dispatcher: ApiRequestDispatcher,
) : TeleportApi {

    override fun travel(
        request: PlayerTeleportRequest,
        callback: ApiCallback<TeleportActionOutcome>,
    ): ApiRequestHandle = dispatcher.submit(
        task = {
            ApiCallResult.Completed(
                port.travel(
                    request.playerId,
                    request.target.toMovementTarget()
                ).toApi()
            )
        },
        deliver = callback::onComplete,
    )

}
