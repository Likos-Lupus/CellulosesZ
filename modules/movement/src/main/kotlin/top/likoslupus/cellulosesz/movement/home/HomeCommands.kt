package top.likoslupus.cellulosesz.movement.home

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

internal object HomeCommands {

    fun commands(
        service: HomeService,
        backend: TeleportBackend,
        teleports: TeleportCoordinator,
        teleportSettings: () -> TeleportSettings,
        kernel: RuntimeKernel,
        messages: LocalizedMessages,
        adventure: AdventureRuntime,
        presentation: TextPresentation,
    ): List<CommandDefinition> = listOf(
        command(
            name = "sethome",
            category = CommandCategory.MOVEMENT,
            permission = CommandPermissions.SET_HOME,
            documentation = "movement/sethome",
        ) {
            executesPlayer {
                setHome(
                    context,
                    null,
                    service,
                    backend,
                    kernel,
                    messages,
                    adventure,
                    presentation,
                )
            }
            argument("name", word()) { name ->
                executesPlayer {
                    setHome(
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
            name = "home",
            category = CommandCategory.MOVEMENT,
            permission = CommandPermissions.HOME,
            documentation = "movement/home",
        ) {
            executesPlayer {
                goHome(
                    context,
                    null,
                    service,
                    teleports,
                    teleportSettings,
                    kernel,
                    messages,
                    adventure,
                    presentation,
                )
            }
            argument("name", word()) { name ->
                executesPlayer {
                    goHome(
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
            name = "delhome",
            category = CommandCategory.MOVEMENT,
            permission = CommandPermissions.DEL_HOME,
            documentation = "movement/delhome",
        ) {
            argument("name", word()) { name ->
                executesPlayer {
                    deleteHome(
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
        command(
            name = "homes",
            category = CommandCategory.MOVEMENT,
            permission = CommandPermissions.HOMES,
            documentation = "movement/homes",
        ) {
            executesPlayer {
                listHomes(
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

    private fun setHome(
        context: CommandContext<CommandSourceStack>,
        rawName: String?,
        service: HomeService,
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
            MessageKey("movement.pending.saving_home")
        ) { target ->
            val notice = when (val position = backend.position(playerId)) {
                null -> MovementMessages.playerOffline()
                else -> MovementMessages.homeSet(service.set(playerId, rawName, position))
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

    private fun goHome(
        context: CommandContext<CommandSourceStack>,
        rawName: String?,
        service: HomeService,
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
            val notices = when (val result = service.get(playerId, rawName)) {
                is HomeLookupResult.Found -> {
                    val intent = TeleportIntent(
                        subjectId = playerId,
                        destination = TeleportDestination.Fixed(result.home.position),
                        cause = TeleportCause.HOME,
                        policy = teleportPolicyFor(
                            TeleportCause.HOME,
                            teleportSettings()
                        ),
                    )
                    when (val outcome = teleports.execute(intent)) {
                        is TeleportOutcome.Success -> listOf(MovementMessages.teleportToHome(result.home))
                        else -> listOfNotNull(MovementMessages.teleportOutcome(outcome))
                    }
                }

                HomeLookupResult.InvalidName,
                HomeLookupResult.NotFound -> listOfNotNull(MovementMessages.homeLookupError(result))
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

    private fun deleteHome(
        context: CommandContext<CommandSourceStack>,
        rawName: String,
        service: HomeService,
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
            MessageKey("movement.pending.deleting_home")
        ) { target ->
            val notice = MovementMessages.homeDelete(
                rawName,
                service.delete(playerId, rawName)
            )
            kernel.sendNotices(
                target,
                adventure,
                presentation,
                playerId,
                listOf(notice)
            )
        }
    }

    private fun listHomes(
        context: CommandContext<CommandSourceStack>,
        service: HomeService,
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
            MessageKey("movement.pending.loading_homes")
        ) { target ->
            kernel.sendPage(
                target,
                adventure,
                presentation,
                playerId,
                MovementMessages.homes(service.list(playerId))
            )
        }
    }

}
