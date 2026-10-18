package top.likoslupus.cellulosesz.movement.teleport.command

import top.likoslupus.cellulosesz.core.command.dsl.CommandDefinition
import top.likoslupus.cellulosesz.core.permission.PermissionService
import top.likoslupus.cellulosesz.core.preference.PlayerPreferenceService
import top.likoslupus.cellulosesz.core.runtime.RuntimeKernel
import top.likoslupus.cellulosesz.core.text.adventure.AdventureRuntime
import top.likoslupus.cellulosesz.core.text.i18n.LocalizedMessages
import top.likoslupus.cellulosesz.core.text.presentation.TextPresentation
import top.likoslupus.cellulosesz.movement.config.HistorySettings
import top.likoslupus.cellulosesz.movement.config.TeleportSettings
import top.likoslupus.cellulosesz.movement.teleport.TeleportCoordinator
import top.likoslupus.cellulosesz.movement.teleport.history.TeleportHistoryService

/**
 * Direct teleport commands. All of them funnel through [TeleportCoordinator].
 *
 * The declarations themselves live next to their handlers, one object per responsibility
 * ([BackCommand], [DirectTeleportCommands]); this entry point only assembles them in registration
 * order. Shared intent construction is in `TeleportCommandSupport.kt`.
 */
internal object TeleportCommands {

    fun commands(
        teleports: TeleportCoordinator,
        history: TeleportHistoryService,
        teleportSettings: () -> TeleportSettings,
        historySettings: () -> HistorySettings,
        kernel: RuntimeKernel,
        messages: LocalizedMessages,
        adventure: AdventureRuntime,
        presentation: TextPresentation,
        permissions: PermissionService,
        preferences: PlayerPreferenceService,
    ): List<CommandDefinition> =
        BackCommand.commands(
            teleports,
            history,
            teleportSettings,
            historySettings,
            kernel,
            messages,
            adventure,
            presentation,
            permissions,
        ) + DirectTeleportCommands.commands(
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
