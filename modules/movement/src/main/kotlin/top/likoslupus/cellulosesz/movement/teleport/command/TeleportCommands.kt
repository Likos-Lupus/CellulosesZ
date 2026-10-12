package top.likoslupus.cellulosesz.movement.teleport.command

import com.mojang.brigadier.context.CommandContext
import net.minecraft.commands.CommandSourceStack
import net.minecraft.resources.Identifier
import top.likoslupus.cellulosesz.core.command.CommandCategory
import top.likoslupus.cellulosesz.core.command.CommandFeedbackTarget
import top.likoslupus.cellulosesz.core.command.dsl.CommandDefinition
import top.likoslupus.cellulosesz.core.command.dsl.command
import top.likoslupus.cellulosesz.core.command.dsl.double
import top.likoslupus.cellulosesz.core.command.dsl.word
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
import top.likoslupus.cellulosesz.movement.teleport.*
import top.likoslupus.cellulosesz.movement.teleport.history.TeleportHistoryService
import java.util.*

/** Direct teleport commands. All of them funnel through [TeleportCoordinator]. */
internal object TeleportCommands {

    fun commands(
        teleports: TeleportCoordinator,
        history: TeleportHistoryService,
        teleportSettings: () -> TeleportSettings,
        kernel: RuntimeKernel,
        messages: LocalizedMessages,
        adventure: AdventureRuntime,
        presentation: TextPresentation,
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
                    kernel,
                    messages,
                    adventure,
                    presentation,
                )
            }
        },

        command(
            name = "tp",
            category = CommandCategory.MOVEMENT,
            permission = CommandPermissions.TP,
            documentation = "movement/tp",
        ) {
            argument("first", word()) { first ->
                executes {
                    direct(
                        context,
                        get(first),
                        null,
                        teleports,
                        teleportSettings,
                        kernel,
                        messages,
                        adventure,
                        presentation,
                    )
                }
                argument("second", word()) { second ->
                    executes {
                        direct(
                            context,
                            get(first),
                            get(second),
                            teleports,
                            teleportSettings,
                            kernel,
                            messages,
                            adventure,
                            presentation,
                        )
                    }
                }
            }
        },

        command(
            name = "tphere",
            category = CommandCategory.MOVEMENT,
            permission = CommandPermissions.TP_HERE,
            documentation = "movement/tphere",
        ) {
            argument("target", word()) { target ->
                executesPlayer {
                    teleportHere(
                        context,
                        get(target),
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
            name = "tppos",
            category = CommandCategory.MOVEMENT,
            permission = CommandPermissions.TP_POS,
            documentation = "movement/tppos",
        ) {
            argument("x", double()) { x ->
                argument("y", double()) { y ->
                    argument("z", double()) { z ->
                        executesPlayer {
                            teleportPos(
                                context,
                                null,
                                get(x),
                                get(y),
                                get(z),
                                teleports,
                                teleportSettings,
                                kernel,
                                messages,
                                adventure,
                                presentation,
                            )
                        }
                        argument("dimension", word()) { dimension ->
                            executesPlayer {
                                teleportPos(
                                    context,
                                    get(dimension),
                                    get(x),
                                    get(y),
                                    get(z),
                                    teleports,
                                    teleportSettings,
                                    kernel,
                                    messages,
                                    adventure,
                                    presentation,
                                )
                            }
                        }
                    }
                }
            }
        },
    )

    private fun back(
        context: CommandContext<CommandSourceStack>,
        teleports: TeleportCoordinator,
        history: TeleportHistoryService,
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
            val notices = when (val previous = history.previous(playerId)) {
                null -> listOf(MovementMessages.noPreviousLocation())
                else -> when (
                    val outcome = teleports.execute(
                        intent(
                            playerId,
                            previous,
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

    private fun direct(
        context: CommandContext<CommandSourceStack>,
        firstName: String,
        secondName: String?,
        teleports: TeleportCoordinator,
        teleportSettings: () -> TeleportSettings,
        kernel: RuntimeKernel,
        messages: LocalizedMessages,
        adventure: AdventureRuntime,
        presentation: TextPresentation,
    ): Int {
        val source = context.source
        if (secondName == null) {
            val subject = source.player
                ?: return source.replyError(
                    adventure,
                    messages.renderFor(
                        null,
                        MessageKey("core.error.player_required")
                    )
                )
            val target = PlayerResolver.onlineByName(source.server, firstName)
                ?: return source.replyError(
                    adventure,
                    messages.renderFor(
                        subject.uuid,
                        MessageKey("movement.teleport.player_not_online"),
                        firstName
                    ),
                )

            return when (subject.uuid) {
                target.uuid -> source.replyError(
                    adventure,
                    messages.renderFor(
                        subject.uuid,
                        MessageKey("movement.teleport.already_there")
                    ),
                )

                else -> launch(
                    source,
                    subject.uuid,
                    target.uuid,
                    TeleportCause.DIRECT,
                    teleports,
                    teleportSettings,
                    kernel,
                    messages,
                    adventure,
                    presentation,
                )
            }
        }

        val subject = PlayerResolver.onlineByName(source.server, firstName)
            ?: return source.replyError(
                adventure,
                messages.renderFor(
                    source.player?.uuid,
                    MessageKey("movement.teleport.player_not_online"),
                    firstName
                ),
            )
        val target = PlayerResolver.onlineByName(source.server, secondName)
            ?: return source.replyError(
                adventure,
                messages.renderFor(
                    source.player?.uuid,
                    MessageKey("movement.teleport.player_not_online"),
                    secondName
                ),
            )
        return launch(
            source,
            subject.uuid,
            target.uuid,
            TeleportCause.ADMIN,
            teleports,
            teleportSettings,
            kernel,
            messages,
            adventure,
            presentation,
        )
    }

    private fun teleportHere(
        context: CommandContext<CommandSourceStack>,
        targetName: String,
        teleports: TeleportCoordinator,
        teleportSettings: () -> TeleportSettings,
        kernel: RuntimeKernel,
        messages: LocalizedMessages,
        adventure: AdventureRuntime,
        presentation: TextPresentation,
    ): Int {
        val source = context.source
        val executor = source.player
            ?: return source.replyError(
                adventure,
                messages.renderFor(
                    null,
                    MessageKey("core.error.player_required")
                )
            )
        val target = PlayerResolver.onlineByName(source.server, targetName)
            ?: return source.replyError(
                adventure,
                messages.renderFor(
                    executor.uuid,
                    MessageKey("movement.teleport.player_not_online"),
                    targetName
                ),
            )
        return launch(
            source,
            target.uuid,
            executor.uuid,
            TeleportCause.ADMIN,
            teleports,
            teleportSettings,
            kernel,
            messages,
            adventure,
            presentation,
        )
    }

    private fun teleportPos(
        context: CommandContext<CommandSourceStack>,
        dimension: String?,
        x: Double,
        y: Double,
        z: Double,
        teleports: TeleportCoordinator,
        teleportSettings: () -> TeleportSettings,
        kernel: RuntimeKernel,
        messages: LocalizedMessages,
        adventure: AdventureRuntime,
        presentation: TextPresentation,
    ): Int {
        val source = context.source
        val executor = source.player
            ?: return source.replyError(
                adventure,
                messages.renderFor(
                    null,
                    MessageKey("core.error.player_required")
                )
            )

        val resolvedDimension = dimension
            ?: executor.level().dimension().identifier().toString()
        if (Identifier.tryParse(resolvedDimension) == null) {
            return source.replyError(
                adventure,
                messages.renderFor(
                    executor.uuid,
                    MessageKey("movement.teleport.invalid_dimension"),
                    resolvedDimension
                ),
            )
        }

        val position = StoredPosition(
            dimension = resolvedDimension,
            x = x,
            y = y,
            z = z,
            yaw = executor.yRot,
            pitch = executor.xRot,
        )
        return launch(
            source,
            executor.uuid,
            position,
            TeleportCause.ADMIN,
            teleports,
            teleportSettings,
            kernel,
            messages,
            adventure,
            presentation,
        )
    }

    private fun launch(
        source: CommandSourceStack,
        subjectId: UUID,
        targetId: UUID,
        cause: TeleportCause,
        teleports: TeleportCoordinator,
        teleportSettings: () -> TeleportSettings,
        kernel: RuntimeKernel,
        messages: LocalizedMessages,
        adventure: AdventureRuntime,
        presentation: TextPresentation,
    ): Int = launchDestination(
        source,
        subjectId,
        TeleportDestination.Player(targetId),
        cause,
        teleports,
        teleportSettings,
        kernel,
        messages,
        adventure,
        presentation,
    )

    private fun launch(
        source: CommandSourceStack,
        subjectId: UUID,
        position: StoredPosition,
        cause: TeleportCause,
        teleports: TeleportCoordinator,
        teleportSettings: () -> TeleportSettings,
        kernel: RuntimeKernel,
        messages: LocalizedMessages,
        adventure: AdventureRuntime,
        presentation: TextPresentation,
    ): Int = launchDestination(
        source,
        subjectId,
        TeleportDestination.Fixed(position),
        cause,
        teleports,
        teleportSettings,
        kernel,
        messages,
        adventure,
        presentation,
    )

    private fun launchDestination(
        source: CommandSourceStack,
        subjectId: UUID,
        destination: TeleportDestination,
        cause: TeleportCause,
        teleports: TeleportCoordinator,
        teleportSettings: () -> TeleportSettings,
        kernel: RuntimeKernel,
        messages: LocalizedMessages,
        adventure: AdventureRuntime,
        presentation: TextPresentation,
    ): Int =
        movementLaunch(
            source,
            kernel,
            messages,
            adventure,
            MessageKey("movement.pending.teleport")
        ) { _ ->
            val outcome = teleports.execute(
                intent(
                    subjectId,
                    destination,
                    cause,
                    teleportSettings
                )
            )
            val notices = when (outcome) {
                is TeleportOutcome.Success -> listOf(MovementMessages.teleportComplete())
                else -> listOfNotNull(MovementMessages.teleportOutcome(outcome))
            }
            kernel.sendNotices(
                CommandFeedbackTarget.Player(subjectId),
                adventure,
                presentation,
                subjectId,
                notices,
            )
        }

    private fun intent(
        subjectId: UUID,
        position: StoredPosition,
        cause: TeleportCause,
        teleportSettings: () -> TeleportSettings,
    ): TeleportIntent =
        intent(
            subjectId,
            TeleportDestination.Fixed(position),
            cause,
            teleportSettings
        )

    private fun intent(
        subjectId: UUID,
        destination: TeleportDestination,
        cause: TeleportCause,
        teleportSettings: () -> TeleportSettings,
    ): TeleportIntent = TeleportIntent(
        subjectId = subjectId,
        destination = destination,
        cause = cause,
        policy = teleportPolicyFor(cause, teleportSettings()),
    )

}
