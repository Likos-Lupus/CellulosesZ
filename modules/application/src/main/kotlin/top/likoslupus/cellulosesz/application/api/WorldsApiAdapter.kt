package top.likoslupus.cellulosesz.application.api

import top.likoslupus.cellulosesz.api.ApiPosition
import top.likoslupus.cellulosesz.api.ApiQuery
import top.likoslupus.cellulosesz.api.actor.AddonAccess
import top.likoslupus.cellulosesz.api.common.ApiCallResult
import top.likoslupus.cellulosesz.api.common.ApiCallback
import top.likoslupus.cellulosesz.api.common.ApiRequestHandle
import top.likoslupus.cellulosesz.api.movement.LastLocationOutcome
import top.likoslupus.cellulosesz.api.movement.WorldView
import top.likoslupus.cellulosesz.api.movement.WorldsApi
import top.likoslupus.cellulosesz.application.api.extension.ApplicationExtensionHub
import top.likoslupus.cellulosesz.core.permission.PermissionService
import top.likoslupus.cellulosesz.core.permission.PermissionSpec
import top.likoslupus.cellulosesz.core.permission.VanillaPermissionFallback
import top.likoslupus.cellulosesz.core.runtime.RuntimeKernel
import top.likoslupus.cellulosesz.movement.config.MovementSettings
import top.likoslupus.cellulosesz.movement.integration.MovementIntegrationPort
import top.likoslupus.cellulosesz.movement.integration.MovementLastLocationResult
import java.util.*

/**
 * Reads world names and the last persisted position without exposing Minecraft types. World
 * listing/access are ordinary reads; the last known position is a privacy-sensitive fact and needs
 * the `movement.location.read` capability plus the registered addon handle.
 */
internal class WorldsApiAdapter(
    private val kernel: RuntimeKernel,
    private val integration: MovementIntegrationPort,
    private val permissions: PermissionService,
    private val settings: () -> MovementSettings,
    private val hub: ApplicationExtensionHub,
    private val dispatcher: ApiRequestDispatcher,
) : WorldsApi {

    override fun list(): ApiQuery<List<WorldView>> {
        val aliasByDimension = settings().worlds.aliases.entries.associate { (alias, id) -> id to alias }
        return ApiQuery.Available(
            kernel.requireServer().allLevels.map { level ->
                val id = level.dimension().identifier().toString()
                WorldView(id, aliasByDimension[id])
            }
        )
    }

    override fun find(worldId: String): ApiQuery<WorldView?> =
        when (val worlds = list()) {
            is ApiQuery.Available -> ApiQuery.Available(worlds.value.firstOrNull { it.id == worldId })
            ApiQuery.NotReady -> ApiQuery.NotReady
            ApiQuery.Stopping -> ApiQuery.Stopping
        }

    override fun canAccess(playerId: UUID, worldId: String): ApiQuery<Boolean> {
        if (!settings().worldAccess.enabled) {
            return ApiQuery.Available(true)
        }
        val player = kernel.requireServer().playerList.getPlayer(playerId)
            ?: return ApiQuery.Available(false)
        return ApiQuery.Available(
            permissions.allows(
                player.createCommandSourceStack(),
                PermissionSpec(
                    "cellulosesz.world.${worldId.replace(':', '.')}",
                    VanillaPermissionFallback.DENY_ALL,
                    "Teleport into $worldId",
                ),
            )
        )
    }

    override fun lastKnownLocation(
        access: AddonAccess,
        playerId: UUID,
        callback: ApiCallback<LastLocationOutcome>,
    ): ApiRequestHandle = dispatcher.submit(
        task = {
            when {
                !hub.isAuthorizedAccess(access, CAP_LOCATION) ->
                    ApiCallResult.Completed(LastLocationOutcome.NotAuthorized)

                else -> when (val result = integration.lastKnownLocation(playerId)) {
                    is MovementLastLocationResult.Found -> {
                        val position = result.position
                        ApiCallResult.Completed(
                            LastLocationOutcome.Found(
                                ApiPosition(
                                    position.dimension,
                                    position.x,
                                    position.y,
                                    position.z,
                                    position.yaw,
                                    position.pitch,
                                )
                            )
                        )
                    }

                    MovementLastLocationResult.Unknown ->
                        ApiCallResult.Completed(LastLocationOutcome.Unknown)

                    MovementLastLocationResult.StorageUnavailable ->
                        ApiCallResult.Unavailable("movement.location")
                }
            }
        },
        deliver = callback::onComplete,
    )

    private companion object {

        private const val CAP_LOCATION = "movement.location.read"

    }

}
