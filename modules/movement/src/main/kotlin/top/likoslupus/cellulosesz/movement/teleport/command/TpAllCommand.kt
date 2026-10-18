package top.likoslupus.cellulosesz.movement.teleport.command

import com.mojang.brigadier.context.CommandContext
import net.minecraft.commands.CommandSourceStack
import top.likoslupus.cellulosesz.core.command.CommandCategory
import top.likoslupus.cellulosesz.core.command.dsl.CommandDefinition
import top.likoslupus.cellulosesz.core.command.dsl.command
import top.likoslupus.cellulosesz.core.command.dsl.word
import top.likoslupus.cellulosesz.core.command.onlineNames
import top.likoslupus.cellulosesz.core.permission.CommandPermissions
import top.likoslupus.cellulosesz.core.player.PlayerResolver
import top.likoslupus.cellulosesz.core.runtime.RuntimeKernel
import top.likoslupus.cellulosesz.core.text.MessageKey
import top.likoslupus.cellulosesz.core.text.adventure.AdventureRuntime
import top.likoslupus.cellulosesz.core.text.adventure.replyError
import top.likoslupus.cellulosesz.core.text.i18n.LocalizedMessages
import top.likoslupus.cellulosesz.core.text.i18n.renderFor
import top.likoslupus.cellulosesz.core.text.presentation.TextPresentation
import top.likoslupus.cellulosesz.core.text.presentation.sendNotices
import top.likoslupus.cellulosesz.movement.command.movementLaunch
import top.likoslupus.cellulosesz.movement.config.TeleportSettings
import top.likoslupus.cellulosesz.movement.format.MovementMessages
import top.likoslupus.cellulosesz.movement.format.teleportedAll
import top.likoslupus.cellulosesz.movement.teleport.TeleportCoordinator
import top.likoslupus.cellulosesz.movement.teleport.TeleportDestination
import top.likoslupus.cellulosesz.movement.teleport.TeleportOutcome

/** `/tpall [player]`: teleports everyone (except the destination) to a player. */
internal object TpAllCommand {

    fun commands(
        teleports: TeleportCoordinator,
        teleportSettings: () -> TeleportSettings,
        kernel: RuntimeKernel,
        messages: LocalizedMessages,
        adventure: AdventureRuntime,
        presentation: TextPresentation,
    ): List<CommandDefinition> = listOf(
        command(
            name = "tpall",
            category = CommandCategory.MOVEMENT,
            permission = CommandPermissions.TP_ALL,
            documentation = "movement/tpall",
        ) {
            executesPlayer {
                everyoneTo(
                    context,
                    null,
                    teleports,
                    teleportSettings,
                    kernel,
                    messages,
                    adventure,
                    presentation,
                )
            }
            argument("player", word()) { player ->
                suggests { suggestAll(onlineNames(source)) }
                executes {
                    everyoneTo(
                        context,
                        get(player),
                        teleports,
                        teleportSettings,
                        kernel,
                        messages,
                        adventure,
                        presentation,
                    )
                }
            }
        },
    )

    private fun everyoneTo(
        context: CommandContext<CommandSourceStack>,
        destinationName: String?,
        teleports: TeleportCoordinator,
        teleportSettings: () -> TeleportSettings,
        kernel: RuntimeKernel,
        messages: LocalizedMessages,
        adventure: AdventureRuntime,
        presentation: TextPresentation,
    ): Int {
        val source = context.source
        val actorId = source.player?.uuid
        val destinationId = when (destinationName) {
            null -> actorId
            else -> {
                val resolved = PlayerResolver.onlineByName(source.server, destinationName)
                    ?: return source.replyError(
                        adventure,
                        messages.renderFor(
                            actorId,
                            MessageKey("movement.teleport.player_not_online"),
                            destinationName,
                        )
                    )
                resolved.uuid
            }
        }
        if (destinationId == null) {
            return source.replyError(
                adventure,
                messages.renderFor(
                    null,
                    MessageKey("core.error.player_required")
                )
            )
        }
        val resolvedDestination = when (destinationName) {
            null -> source.player?.name?.string
            else -> PlayerResolver.onlineById(source.server, destinationId)?.name?.string
                ?: destinationName
        }
        return movementLaunch(
            source,
            kernel,
            messages,
            adventure,
            MessageKey("movement.pending.teleport"),
        ) { feedback ->
            val subjects = kernel.onServerThread {
                PlayerResolver.online(kernel.requireServer())
                        .map { it.uuid }
                        .filter { it != destinationId }
            }
            var moved = 0
            subjects.forEach { subjectId ->
                val outcome = executeAdminTeleport(
                    subjectId,
                    TeleportDestination.Player(destinationId),
                    teleports,
                    teleportSettings,
                )
                if (outcome is TeleportOutcome.Success) {
                    moved++
                }
            }
            kernel.sendNotices(
                feedback,
                adventure,
                presentation,
                actorId,
                listOf(
                    MovementMessages.teleportedAll(
                        moved,
                        resolvedDestination
                    )
                ),
            )
        }
    }

}
