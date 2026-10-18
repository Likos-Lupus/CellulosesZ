package top.likoslupus.cellulosesz.movement.teleport.command

import com.mojang.brigadier.context.CommandContext
import net.minecraft.commands.CommandSourceStack
import top.likoslupus.cellulosesz.core.command.CommandCategory
import top.likoslupus.cellulosesz.core.command.dsl.CommandDefinition
import top.likoslupus.cellulosesz.core.command.dsl.command
import top.likoslupus.cellulosesz.core.command.requiresPermission
import top.likoslupus.cellulosesz.core.permission.CommandPermissions
import top.likoslupus.cellulosesz.core.permission.PermissionService
import top.likoslupus.cellulosesz.core.runtime.RuntimeKernel
import top.likoslupus.cellulosesz.core.text.MessageKey
import top.likoslupus.cellulosesz.core.text.adventure.AdventureRuntime
import top.likoslupus.cellulosesz.core.text.adventure.replyError
import top.likoslupus.cellulosesz.core.text.i18n.LocalizedMessages
import top.likoslupus.cellulosesz.core.text.presentation.TextPresentation
import top.likoslupus.cellulosesz.core.text.presentation.sendNotices
import top.likoslupus.cellulosesz.movement.command.movementLaunch
import top.likoslupus.cellulosesz.movement.config.HistorySettings
import top.likoslupus.cellulosesz.movement.config.TeleportSettings
import top.likoslupus.cellulosesz.movement.format.MovementMessages
import top.likoslupus.cellulosesz.movement.format.noPreviousLocation
import top.likoslupus.cellulosesz.movement.format.teleportBack
import top.likoslupus.cellulosesz.movement.format.teleportOutcome
import top.likoslupus.cellulosesz.movement.teleport.TeleportCause
import top.likoslupus.cellulosesz.movement.teleport.TeleportCoordinator
import top.likoslupus.cellulosesz.movement.teleport.TeleportOutcome
import top.likoslupus.cellulosesz.movement.teleport.history.BackSelection
import top.likoslupus.cellulosesz.movement.teleport.history.TeleportHistoryService

/** `/back`: returns to the last teleport origin, or the last death point when that is enabled. */
internal object BackCommand {

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
    ): List<CommandDefinition> = listOf(
        command(
            name = "back",
            category = CommandCategory.MOVEMENT,
            permission = CommandPermissions.BACK,
            documentation = "movement/back",
        ) {
            executesPlayer {
                back(
                    context,
                    teleports,
                    history,
                    teleportSettings,
                    historySettings,
                    kernel,
                    messages,
                    adventure,
                    presentation,
                    permissions,
                )
            }
        },
    )

    private fun back(
        context: CommandContext<CommandSourceStack>,
        teleports: TeleportCoordinator,
        history: TeleportHistoryService,
        teleportSettings: () -> TeleportSettings,
        historySettings: () -> HistorySettings,
        kernel: RuntimeKernel,
        messages: LocalizedMessages,
        adventure: AdventureRuntime,
        presentation: TextPresentation,
        permissions: PermissionService,
    ): Int {
        val source = context.source
        val playerId = source.player?.uuid
            ?: return source.replyError(
                adventure,
                messages.renderFor(
                    null,
                    MessageKey("core.error.player_required")
                )
            )

        val config = historySettings()
        val hasDeathPermission = source.requiresPermission(
            permissions,
            CommandPermissions.BACK_ON_DEATH
        )

        return movementLaunch(
            source,
            kernel,
            messages,
            adventure,
            MessageKey("movement.pending.teleport")
        ) { target ->
            val previous = history.previous(playerId)
            val death = if (config.recordDeath && hasDeathPermission) {
                history.death(playerId)
            } else {
                null
            }
            val destination = BackSelection.select(
                teleport = previous,
                death = death,
                recordDeath = config.recordDeath,
                hasDeathPermission = hasDeathPermission,
            )
            val notices = when (destination) {
                null -> listOf(MovementMessages.noPreviousLocation())
                else -> when (
                    val outcome = teleports.execute(
                        teleportIntent(
                            playerId,
                            destination,
                            TeleportCause.BACK,
                            teleportSettings
                        )
                    )
                ) {
                    is TeleportOutcome.Success -> listOf(MovementMessages.teleportBack())
                    else -> listOfNotNull(MovementMessages.teleportOutcome(outcome))
                }
            }
            kernel.sendNotices(
                target,
                adventure,
                presentation,
                playerId,
                notices
            )
        }
    }

}
