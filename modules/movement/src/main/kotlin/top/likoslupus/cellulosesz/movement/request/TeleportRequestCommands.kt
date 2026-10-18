package top.likoslupus.cellulosesz.movement.request

import top.likoslupus.cellulosesz.core.command.dsl.CommandDefinition
import top.likoslupus.cellulosesz.core.permission.PermissionService
import top.likoslupus.cellulosesz.core.preference.PlayerPreferenceService
import top.likoslupus.cellulosesz.core.runtime.RuntimeKernel
import top.likoslupus.cellulosesz.core.text.adventure.AdventureRuntime
import top.likoslupus.cellulosesz.core.text.i18n.LocalizedMessages
import top.likoslupus.cellulosesz.core.text.presentation.TextPresentation
import top.likoslupus.cellulosesz.movement.RequestReachabilityPort
import top.likoslupus.cellulosesz.movement.config.TeleportSettings
import top.likoslupus.cellulosesz.movement.teleport.TeleportCoordinator

/**
 * Entry point for the teleport request commands. Each responsibility owns its definitions in a
 * sibling object; this object only fixes the installation order.
 */
internal object TeleportRequestCommands {

    fun commands(
        service: TeleportRequestService,
        tpAuto: TpAutoService,
        teleports: TeleportCoordinator,
        teleportSettings: () -> TeleportSettings,
        reachability: RequestReachabilityPort,
        kernel: RuntimeKernel,
        messages: LocalizedMessages,
        adventure: AdventureRuntime,
        presentation: TextPresentation,
        permissions: PermissionService,
        preferences: PlayerPreferenceService,
    ): List<CommandDefinition> =
        TeleportRequestSendCommands.commands(
            service,
            tpAuto,
            teleports,
            teleportSettings,
            reachability,
            kernel,
            messages,
            adventure,
            presentation,
            permissions,
        ) + TeleportRequestResponseCommands.commands(
            service,
            teleports,
            teleportSettings,
            kernel,
            messages,
            adventure,
            presentation,
        ) + TeleportAutoCommands.commands(
            tpAuto,
            preferences,
            kernel,
            messages,
            adventure,
            presentation,
        ) + TeleportPreferenceCommands.commands(
            preferences,
            kernel,
            messages,
            adventure,
            presentation,
        )

}
