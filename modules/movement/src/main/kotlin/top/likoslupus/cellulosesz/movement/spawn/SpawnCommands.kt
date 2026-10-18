package top.likoslupus.cellulosesz.movement.spawn

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
import top.likoslupus.cellulosesz.movement.format.*
import top.likoslupus.cellulosesz.movement.teleport.*

internal object SpawnCommands {

    fun commands(
        service: SpawnService,
        backend: TeleportBackend,
        teleports: TeleportCoordinator,
        teleportSettings: () -> TeleportSettings,
        kernel: RuntimeKernel,
        messages: LocalizedMessages,
        adventure: AdventureRuntime,
        presentation: TextPresentation,
    ): List<CommandDefinition> = listOf(
        command(
            name = "spawn",
            category = CommandCategory.MOVEMENT,
            permission = CommandPermissions.SPAWN,
            documentation = "movement/spawn",
        ) {
            executesPlayer {
                teleportToSpawn(
                    context,
                    service,
                    backend,
                    teleports,
                    teleportSettings,
                    kernel,
                    messages,
                    adventure,
                    presentation,
                )
            }
        },

        command(
            name = "setspawn",
            category = CommandCategory.MOVEMENT,
            permission = CommandPermissions.SET_SPAWN,
            documentation = "movement/setspawn",
        ) {
            executesPlayer {
                setSpawn(
                    context,
                    service,
                    backend,
                    kernel,
                    messages,
                    adventure,
                    presentation,
                )
            }
        },

        command(
            name = "delspawn",
            category = CommandCategory.MOVEMENT,
            permission = CommandPermissions.DEL_SPAWN,
            documentation = "movement/delspawn",
        ) {
            executesPlayer {
                deleteSpawn(
                    context,
                    service,
                    kernel,
                    messages,
                    adventure,
                    presentation,
                )
            }
        },
    )

    private fun teleportToSpawn(
        context: CommandContext<CommandSourceStack>,
        service: SpawnService,
        backend: TeleportBackend,
        teleports: TeleportCoordinator,
        teleportSettings: () -> TeleportSettings,
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
                )
            )

        return movementLaunch(
            source,
            kernel,
            messages,
            adventure,
            MessageKey("movement.pending.teleport")
        ) { target ->
            val position = service.configured() ?: backend.vanillaSpawnPosition()
            val intent = TeleportIntent(
                subjectId = playerId,
                destination = TeleportDestination.Fixed(position),
                cause = TeleportCause.SPAWN,
                policy = teleportPolicyFor(
                    TeleportCause.SPAWN,
                    teleportSettings()
                ),
            )
            val notices = when (val outcome = teleports.execute(intent)) {
                is TeleportOutcome.Success -> listOf(MovementMessages.teleportToSpawn())
                else -> listOfNotNull(MovementMessages.teleportOutcome(outcome))
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

    private fun setSpawn(
        context: CommandContext<CommandSourceStack>,
        service: SpawnService,
        backend: TeleportBackend,
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
                )
            )

        return movementLaunch(
            source,
            kernel,
            messages,
            adventure,
            MessageKey("movement.pending.saving_spawn")
        ) { target ->
            val notice = when (val position = backend.position(playerId)) {
                null -> MovementMessages.playerOffline()
                else -> {
                    service.set(position)
                    MovementMessages.spawnSet()
                }
            }
            kernel.sendNotices(
                target,
                adventure,
                presentation,
                playerId,
                listOf(notice)
            )
        }
    }

    private fun deleteSpawn(
        context: CommandContext<CommandSourceStack>,
        service: SpawnService,
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
                )
            )

        return movementLaunch(
            source,
            kernel,
            messages,
            adventure,
            MessageKey("movement.pending.clearing_spawn")
        ) { target ->
            val notice = MovementMessages.spawnClear(service.clear())
            kernel.sendNotices(
                target,
                adventure,
                presentation,
                playerId,
                listOf(notice)
            )
        }
    }

}
