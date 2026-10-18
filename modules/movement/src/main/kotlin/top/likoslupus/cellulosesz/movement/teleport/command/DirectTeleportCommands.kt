package top.likoslupus.cellulosesz.movement.teleport.command

import top.likoslupus.cellulosesz.core.command.dsl.CommandDefinition
import top.likoslupus.cellulosesz.core.permission.PermissionService
import top.likoslupus.cellulosesz.core.preference.PlayerPreferenceService
import top.likoslupus.cellulosesz.core.runtime.RuntimeKernel
import top.likoslupus.cellulosesz.core.text.adventure.AdventureRuntime
import top.likoslupus.cellulosesz.core.text.i18n.LocalizedMessages
import top.likoslupus.cellulosesz.core.text.presentation.TextPresentation
import top.likoslupus.cellulosesz.movement.config.TeleportSettings
import top.likoslupus.cellulosesz.movement.teleport.TeleportCoordinator

/**
 * Direct teleport family entry point: `/tp`, `/tphere` and `/tppos`; every move funnels through
 * [TeleportCoordinator]. The declarations and handlers live in [TpCommand] and
 * [TpHerePosCommands]; shared launch and preference helpers are in `TeleportCommandSupport.kt`.
 */
internal object DirectTeleportCommands {

    fun commands(
        teleports: TeleportCoordinator,
        teleportSettings: () -> TeleportSettings,
        kernel: RuntimeKernel,
        messages: LocalizedMessages,
        adventure: AdventureRuntime,
        presentation: TextPresentation,
        permissions: PermissionService,
        preferences: PlayerPreferenceService,
    ): List<CommandDefinition> =
        TpCommand.commands(
            teleports,
            teleportSettings,
            kernel,
            messages,
            adventure,
            presentation,
            permissions,
            preferences,
        ) + TpHerePosCommands.commands(
            teleports,
            teleportSettings,
            kernel,
            messages,
            adventure,
            presentation,
            permissions,
            preferences,
        )

}
