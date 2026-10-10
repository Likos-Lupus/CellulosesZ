package top.likoslupus.cellulosesz.movement.teleport.command

import com.mojang.brigadier.context.CommandContext
import net.minecraft.commands.CommandSourceStack
import net.minecraft.resources.Identifier
import top.likoslupus.cellulosesz.core.command.CommandCategory
import top.likoslupus.cellulosesz.core.command.dsl.CommandDefinition
import top.likoslupus.cellulosesz.core.command.dsl.command
import top.likoslupus.cellulosesz.core.command.dsl.double
import top.likoslupus.cellulosesz.core.command.dsl.word
import top.likoslupus.cellulosesz.core.command.messagePlayer
import top.likoslupus.cellulosesz.core.command.replyError
import top.likoslupus.cellulosesz.core.permission.CommandPermissions
import top.likoslupus.cellulosesz.core.player.PlayerResolver
import top.likoslupus.cellulosesz.core.runtime.RuntimeKernel
import top.likoslupus.cellulosesz.core.text.Messages
import top.likoslupus.cellulosesz.movement.command.launchResult
import top.likoslupus.cellulosesz.movement.config.TeleportSettings
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
                    kernel
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
                        kernel
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
                            kernel
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
                        kernel
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
    ): Int {
        val source = context.source
        val playerId = source.player?.uuid
            ?: return source.replyError(Messages.prefixed("this command requires a player"))

        val job = kernel.launch {
            val message = when (val previous = history.previous(playerId)) {
                null -> Messages.prefixed("no previous location")
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
                    is TeleportOutcome.Success ->
                        Messages.prefixed("teleported back")

                    else -> TeleportFeedback.failure(outcome)
                        ?: return@launch
                }
            }
            kernel.messagePlayer(playerId, message)
        }

        return launchResult(source, job, "teleporting...")
    }

    private fun direct(
        context: CommandContext<CommandSourceStack>,
        firstName: String,
        secondName: String?,
        teleports: TeleportCoordinator,
        teleportSettings: () -> TeleportSettings,
        kernel: RuntimeKernel,
    ): Int {
        val source = context.source
        if (secondName == null) {
            val subject = source.player
                ?: return source.replyError(Messages.prefixed("this command requires a player"))
            val target = PlayerResolver.onlineByName(source.server, firstName)
                ?: return source.replyError(Messages.prefixed("player '$firstName' is not online"))

            return when (subject.uuid) {
                target.uuid -> source.replyError(Messages.prefixed("you are already there"))
                else -> launch(
                    source,
                    subject.uuid,
                    target.uuid,
                    TeleportCause.DIRECT,
                    teleports,
                    teleportSettings,
                    kernel
                )
            }
        }

        val subject = PlayerResolver.onlineByName(source.server, firstName)
            ?: return source.replyError(Messages.prefixed("player '$firstName' is not online"))
        val target = PlayerResolver.onlineByName(source.server, secondName)
            ?: return source.replyError(Messages.prefixed("player '$secondName' is not online"))
        return launch(
            source,
            subject.uuid,
            target.uuid,
            TeleportCause.ADMIN,
            teleports,
            teleportSettings,
            kernel
        )
    }

    private fun teleportHere(
        context: CommandContext<CommandSourceStack>,
        targetName: String,
        teleports: TeleportCoordinator,
        teleportSettings: () -> TeleportSettings,
        kernel: RuntimeKernel,
    ): Int {
        val source = context.source
        val executor = source.player
            ?: return source.replyError(Messages.prefixed("this command requires a player"))
        val target = PlayerResolver.onlineByName(source.server, targetName)
            ?: return source.replyError(Messages.prefixed("player '$targetName' is not online"))
        return launch(
            source,
            target.uuid,
            executor.uuid,
            TeleportCause.ADMIN,
            teleports,
            teleportSettings,
            kernel
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
    ): Int {
        val source = context.source
        val executor = source.player
            ?: return source.replyError(Messages.prefixed("this command requires a player"))

        val resolvedDimension = dimension
            ?: executor.level().dimension().identifier().toString()
        if (Identifier.tryParse(resolvedDimension) == null) {
            return source.replyError(Messages.prefixed("invalid dimension '$resolvedDimension'"))
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
            kernel
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
    ): Int = launchDestination(
        source,
        subjectId,
        TeleportDestination.Player(targetId),
        cause,
        teleports,
        teleportSettings,
        kernel,
    )

    private fun launch(
        source: CommandSourceStack,
        subjectId: UUID,
        position: StoredPosition,
        cause: TeleportCause,
        teleports: TeleportCoordinator,
        teleportSettings: () -> TeleportSettings,
        kernel: RuntimeKernel,
    ): Int = launchDestination(
        source,
        subjectId,
        TeleportDestination.Fixed(position),
        cause,
        teleports,
        teleportSettings,
        kernel,
    )

    private fun launchDestination(
        source: CommandSourceStack,
        subjectId: UUID,
        destination: TeleportDestination,
        cause: TeleportCause,
        teleports: TeleportCoordinator,
        teleportSettings: () -> TeleportSettings,
        kernel: RuntimeKernel,
    ): Int {
        val job = kernel.launch {
            val outcome = teleports.execute(
                intent(
                    subjectId,
                    destination,
                    cause,
                    teleportSettings
                )
            )
            val message = when (outcome) {
                is TeleportOutcome.Success -> Messages.prefixed("teleport complete")
                else -> TeleportFeedback.failure(outcome) ?: return@launch
            }
            kernel.messagePlayer(subjectId, message)
        }

        return launchResult(source, job, "teleporting...")
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
