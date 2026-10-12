package top.likoslupus.cellulosesz.movement.warp

import com.mojang.brigadier.context.CommandContext
import net.minecraft.commands.CommandSourceStack
import top.likoslupus.cellulosesz.core.command.CommandCategory
import top.likoslupus.cellulosesz.core.command.dsl.CommandDefinition
import top.likoslupus.cellulosesz.core.command.dsl.command
import top.likoslupus.cellulosesz.core.command.dsl.word
import top.likoslupus.cellulosesz.core.permission.CommandPermissions
import top.likoslupus.cellulosesz.core.runtime.RuntimeKernel
import top.likoslupus.cellulosesz.core.text.MessageKey
import top.likoslupus.cellulosesz.core.text.adventure.AdventureRuntime
import top.likoslupus.cellulosesz.core.text.adventure.replyError
import top.likoslupus.cellulosesz.core.text.i18n.LocalizedMessages
import top.likoslupus.cellulosesz.core.text.presentation.TextPresentation
import top.likoslupus.cellulosesz.core.text.presentation.sendNotices
import top.likoslupus.cellulosesz.core.text.presentation.sendPage
import top.likoslupus.cellulosesz.movement.command.movementLaunch
import top.likoslupus.cellulosesz.movement.config.TeleportSettings
import top.likoslupus.cellulosesz.movement.format.MovementMessages
import top.likoslupus.cellulosesz.movement.teleport.*

internal object WarpCommands {

    fun commands(
        service: WarpService,
        backend: TeleportBackend,
        teleports: TeleportCoordinator,
        teleportSettings: () -> TeleportSettings,
        kernel: RuntimeKernel,
        messages: LocalizedMessages,
        adventure: AdventureRuntime,
        presentation: TextPresentation,
    ): List<CommandDefinition> = listOf(
        command(
            name = "warp",
            category = CommandCategory.MOVEMENT,
            permission = CommandPermissions.WARP,
            documentation = "movement/warp",
        ) {
            argument("name", word()) { name ->
                executesPlayer {
                    useWarp(
                        context,
                        get(name),
                        service,
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

        command(
            name = "warps",
            category = CommandCategory.MOVEMENT,
            permission = CommandPermissions.WARPS,
            documentation = "movement/warps",
        ) {
            executesPlayer {
                listWarps(
                    context,
                    service,
                    kernel,
                    messages,
                    adventure,
                    presentation,
                )
            }
        },

        command(
            name = "setwarp",
            category = CommandCategory.MOVEMENT,
            permission = CommandPermissions.SET_WARP,
            documentation = "movement/setwarp",
        ) {
            argument("name", word()) { name ->
                executesPlayer {
                    setWarp(
                        context,
                        get(name),
                        service,
                        backend,
                        kernel,
                        messages,
                        adventure,
                        presentation,
                    )
                }
            }
        },

        command(
            name = "delwarp",
            category = CommandCategory.MOVEMENT,
            permission = CommandPermissions.DEL_WARP,
            documentation = "movement/delwarp",
        ) {
            argument("name", word()) { name ->
                executesPlayer {
                    deleteWarp(
                        context,
                        get(name),
                        service,
                        kernel,
                        messages,
                        adventure,
                        presentation,
                    )
                }
            }
        },
    )

    private fun useWarp(
        context: CommandContext<CommandSourceStack>,
        name: String,
        service: WarpService,
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
            val notices = when (val result = service.get(name)) {
                is WarpLookupResult.Found -> {
                    val intent = TeleportIntent(
                        subjectId = playerId,
                        destination = TeleportDestination.Fixed(result.warp.position),
                        cause = TeleportCause.WARP,
                        policy = teleportPolicyFor(TeleportCause.WARP, teleportSettings()),
                    )
                    when (val outcome = teleports.execute(intent)) {
                        is TeleportOutcome.Success -> listOf(MovementMessages.teleportToWarp(result.warp))
                        else -> listOfNotNull(MovementMessages.teleportOutcome(outcome))
                    }
                }

                WarpLookupResult.InvalidName,
                WarpLookupResult.NotFound -> listOfNotNull(MovementMessages.warpLookupError(result))
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

    private fun listWarps(
        context: CommandContext<CommandSourceStack>,
        service: WarpService,
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
            MessageKey("movement.pending.loading_warps")
        ) { target ->
            kernel.sendPage(
                target,
                adventure,
                presentation,
                playerId,
                MovementMessages.warps(service.list())
            )
        }
    }

    private fun setWarp(
        context: CommandContext<CommandSourceStack>,
        name: String,
        service: WarpService,
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
            MessageKey("movement.pending.saving_warp")
        ) { target ->
            val notice = when (val position = backend.position(playerId)) {
                null -> MovementMessages.playerOffline()
                else -> MovementMessages.warpSet(name, service.set(name, position))
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

    private fun deleteWarp(
        context: CommandContext<CommandSourceStack>,
        name: String,
        service: WarpService,
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
            MessageKey("movement.pending.deleting_warp")
        ) { target ->
            val notice = MovementMessages.warpDelete(name, service.delete(name))
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
