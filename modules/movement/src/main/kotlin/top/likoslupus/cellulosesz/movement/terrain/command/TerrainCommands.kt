package top.likoslupus.cellulosesz.movement.terrain.command

import com.mojang.brigadier.context.CommandContext
import net.minecraft.commands.CommandSourceStack
import top.likoslupus.cellulosesz.core.command.CommandCategory
import top.likoslupus.cellulosesz.core.command.dsl.CommandDefinition
import top.likoslupus.cellulosesz.core.command.dsl.command
import top.likoslupus.cellulosesz.core.permission.CommandPermissions
import top.likoslupus.cellulosesz.core.runtime.RuntimeKernel
import top.likoslupus.cellulosesz.core.text.MessageKey
import top.likoslupus.cellulosesz.core.text.adventure.AdventureRuntime
import top.likoslupus.cellulosesz.core.text.adventure.replyError
import top.likoslupus.cellulosesz.core.text.i18n.LocalizedMessages
import top.likoslupus.cellulosesz.core.text.presentation.TextPresentation
import top.likoslupus.cellulosesz.core.text.presentation.sendNotices
import top.likoslupus.cellulosesz.movement.command.movementLaunch
import top.likoslupus.cellulosesz.movement.config.TeleportSettings
import top.likoslupus.cellulosesz.movement.config.TerrainSettings
import top.likoslupus.cellulosesz.movement.format.MovementMessages
import top.likoslupus.cellulosesz.movement.teleport.*
import top.likoslupus.cellulosesz.movement.terrain.TerrainBackend
import top.likoslupus.cellulosesz.movement.terrain.TerrainResult
import top.likoslupus.cellulosesz.movement.terrain.TerrainTarget

/** `/top`, `/bottom` and `/jump`. They resolve a column, then funnel through [TeleportCoordinator]. */
internal object TerrainCommands {

    fun commands(
        backend: TerrainBackend,
        teleports: TeleportCoordinator,
        teleportSettings: () -> TeleportSettings,
        terrainSettings: () -> TerrainSettings,
        kernel: RuntimeKernel,
        messages: LocalizedMessages,
        adventure: AdventureRuntime,
        presentation: TextPresentation,
    ): List<CommandDefinition> = listOf(
        command(
            name = "top",
            category = CommandCategory.MOVEMENT,
            permission = CommandPermissions.TOP,
            documentation = "movement/top",
        ) {
            executesPlayer {
                teleportTerrain(
                    context,
                    TerrainTarget.TOP,
                    backend,
                    teleports,
                    teleportSettings,
                    terrainSettings,
                    kernel,
                    messages,
                    adventure,
                    presentation,
                )
            }
        },

        command(
            name = "bottom",
            category = CommandCategory.MOVEMENT,
            permission = CommandPermissions.BOTTOM,
            documentation = "movement/bottom",
        ) {
            executesPlayer {
                teleportTerrain(
                    context,
                    TerrainTarget.BOTTOM,
                    backend,
                    teleports,
                    teleportSettings,
                    terrainSettings,
                    kernel,
                    messages,
                    adventure,
                    presentation,
                )
            }
        },

        command(
            name = "jump",
            category = CommandCategory.MOVEMENT,
            permission = CommandPermissions.JUMP,
            documentation = "movement/jump",
        ) {
            executesPlayer {
                teleportTerrain(
                    context,
                    TerrainTarget.JUMP,
                    backend,
                    teleports,
                    teleportSettings,
                    terrainSettings,
                    kernel,
                    messages,
                    adventure,
                    presentation,
                )
            }
        },
    )

    private fun teleportTerrain(
        context: CommandContext<CommandSourceStack>,
        target: TerrainTarget,
        backend: TerrainBackend,
        teleports: TeleportCoordinator,
        teleportSettings: () -> TeleportSettings,
        terrainSettings: () -> TerrainSettings,
        kernel: RuntimeKernel,
        messages: LocalizedMessages,
        adventure: AdventureRuntime,
        presentation: TextPresentation,
    ): Int {
        val source = context.source
        val playerId = source.player?.uuid
            ?: return source.replyError(
                adventure,
                messages.renderFor(
                    null,
                    MessageKey("core.error.player_required")
                ),
            )

        return movementLaunch(
            source,
            kernel,
            messages,
            adventure,
            MessageKey("movement.pending.terrain"),
        ) { feedback ->
            val notices = when (val result = backend.destination(
                playerId,
                target,
                terrainSettings()
            )) {
                is TerrainResult.Found -> {
                    val cause = causeFor(target)
                    when (
                        val outcome = teleports.execute(
                            TeleportIntent(
                                subjectId = playerId,
                                destination = TeleportDestination.Fixed(result.position),
                                cause = cause,
                                policy = teleportPolicyFor(cause, teleportSettings()),
                            )
                        )
                    ) {
                        is TeleportOutcome.Success -> listOf(
                            MovementMessages.terrainTeleported(
                                target
                            )
                        )

                        else -> listOfNotNull(MovementMessages.teleportOutcome(outcome))
                    }
                }

                else -> listOfNotNull(MovementMessages.terrainError(result))
            }
            kernel.sendNotices(feedback, adventure, presentation, playerId, notices)
        }
    }

    private fun causeFor(target: TerrainTarget): TeleportCause =
        when (target) {
            TerrainTarget.TOP -> TeleportCause.TOP
            TerrainTarget.BOTTOM -> TeleportCause.BOTTOM
            TerrainTarget.JUMP -> TeleportCause.JUMP
        }

}
