package top.likoslupus.cellulosesz.application.api

import top.likoslupus.cellulosesz.api.common.ApiCallResult
import top.likoslupus.cellulosesz.api.common.ApiCallback
import top.likoslupus.cellulosesz.api.common.ApiRequestHandle
import top.likoslupus.cellulosesz.api.movement.*
import top.likoslupus.cellulosesz.movement.integration.*
import java.util.*

internal class MovementHomesAdapter(
    private val port: MovementIntegrationPort,
    private val dispatcher: ApiRequestDispatcher,
) : HomesApi {

    override fun listOwned(
        ownerId: UUID,
        callback: ApiCallback<HomeListOutcome>,
    ): ApiRequestHandle = dispatcher.submit(
        task = {
            ApiCallResult.Completed(
                port.listHomes(
                    ownerId
                ).toApi()
            )
        },
        deliver = callback::onComplete,
    )

    override fun findOwned(
        ownerId: UUID,
        name: String?,
        callback: ApiCallback<HomeLookupOutcome>,
    ): ApiRequestHandle = dispatcher.submit(
        task = {
            ApiCallResult.Completed(
                port.findHome(
                    ownerId,
                    name
                ).toApi()
            )
        },
        deliver = callback::onComplete,
    )

    override fun setAtCurrentPosition(
        actorId: UUID,
        name: String?,
        callback: ApiCallback<HomeMutationOutcome>,
    ): ApiRequestHandle = dispatcher.submit(
        task = {
            ApiCallResult.Completed(
                port.setHomeAtCurrentPosition(
                    actorId,
                    name
                ).toApi()
            )
        },
        deliver = callback::onComplete,
    )

    override fun renameOwn(
        actorId: UUID,
        oldName: String,
        newName: String,
        callback: ApiCallback<HomeMutationOutcome>,
    ): ApiRequestHandle = dispatcher.submit(
        task = {
            ApiCallResult.Completed(
                port.renameHome(
                    actorId,
                    oldName,
                    newName
                ).toApi()
            )
        },
        deliver = callback::onComplete,
    )

    override fun deleteOwn(
        actorId: UUID,
        name: String,
        callback: ApiCallback<HomeMutationOutcome>,
    ): ApiRequestHandle = dispatcher.submit(
        task = {
            ApiCallResult.Completed(
                port.deleteHome(
                    actorId,
                    name
                ).toApi()
            )
        },
        deliver = callback::onComplete,
    )

}
