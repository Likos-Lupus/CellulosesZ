package top.likoslupus.cellulosesz.application.api

import top.likoslupus.cellulosesz.api.common.ApiCallResult
import top.likoslupus.cellulosesz.api.common.ApiCallback
import top.likoslupus.cellulosesz.api.common.ApiRequestHandle
import top.likoslupus.cellulosesz.api.movement.*
import top.likoslupus.cellulosesz.movement.integration.*
import java.util.*

internal class MovementRequestsAdapter(
    private val port: MovementIntegrationPort,
    private val dispatcher: ApiRequestDispatcher,
) : TeleportRequestApi {

    override fun send(
        playerId: UUID,
        targetId: UUID,
        callback: ApiCallback<TeleportRequestActionOutcome>,
    ): ApiRequestHandle = dispatcher.submit(
        task = {
            ApiCallResult.Completed(
                port.sendTeleportRequest(
                    playerId,
                    targetId
                ).toApi()
            )
        },
        deliver = callback::onComplete,
    )

    override fun accept(
        playerId: UUID,
        requestId: String,
        callback: ApiCallback<TeleportRequestActionOutcome>,
    ): ApiRequestHandle = dispatcher.submit(
        task = {
            ApiCallResult.Completed(
                port.acceptTeleportRequest(
                    playerId,
                    requestId
                ).toApi()
            )
        },
        deliver = callback::onComplete,
    )

    override fun deny(
        playerId: UUID,
        requestId: String,
        callback: ApiCallback<TeleportRequestActionOutcome>,
    ): ApiRequestHandle = dispatcher.submit(
        task = {
            ApiCallResult.Completed(
                port.denyTeleportRequest(
                    playerId,
                    requestId
                ).toApi()
            )
        },
        deliver = callback::onComplete,
    )

    override fun cancelOwn(
        playerId: UUID,
        requestId: String,
        callback: ApiCallback<TeleportRequestActionOutcome>,
    ): ApiRequestHandle = dispatcher.submit(
        task = {
            ApiCallResult.Completed(
                port.cancelOwnTeleportRequest(
                    playerId,
                    requestId
                ).toApi()
            )
        },
        deliver = callback::onComplete,
    )

}
