package top.likoslupus.cellulosesz.movement.teleport.command

import com.mojang.brigadier.CommandDispatcher
import com.mojang.brigadier.arguments.DoubleArgumentType
import com.mojang.brigadier.arguments.StringArgumentType
import com.mojang.brigadier.context.CommandContext
import net.minecraft.commands.CommandSourceStack
import net.minecraft.commands.Commands
import net.minecraft.resources.Identifier
import top.likoslupus.cellulosesz.core.command.messagePlayer
import top.likoslupus.cellulosesz.core.command.replyError
import top.likoslupus.cellulosesz.core.command.requiresPermission
import top.likoslupus.cellulosesz.core.permission.CommandPermissions
import top.likoslupus.cellulosesz.core.permission.PermissionService
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

    fun register(
        dispatcher: CommandDispatcher<CommandSourceStack>,
        teleports: TeleportCoordinator,
        history: TeleportHistoryService,
        teleportSettings: () -> TeleportSettings,
        kernel: RuntimeKernel,
        permissions: PermissionService,
    ) {
        dispatcher.register(
            Commands.literal("back")
                    .executes { context ->
                        back(
                            context,
                            teleports,
                            history,
                            teleportSettings,
                            kernel
                        )
                    }
        )
        dispatcher.register(
            Commands.literal("tp")
                    .then(
                        Commands.argument("first", StringArgumentType.word())
                                .executes { context ->
                                    direct(
                                        context,
                                        StringArgumentType.getString(context, "first"),
                                        null,
                                        teleports,
                                        teleportSettings,
                                        kernel,
                                        permissions,
                                    )
                                }
                                .then(
                                    Commands.argument("second", StringArgumentType.word())
                                            .executes { context ->
                                                direct(
                                                    context,
                                                    StringArgumentType.getString(context, "first"),
                                                    StringArgumentType.getString(context, "second"),
                                                    teleports,
                                                    teleportSettings,
                                                    kernel,
                                                    permissions,
                                                )
                                            }
                                )
                    )
        )
        dispatcher.register(
            Commands.literal("tphere")
                    .requires { it.requiresPermission(permissions, CommandPermissions.TP_HERE) }
                    .then(
                        Commands.argument("target", StringArgumentType.word())
                                .executes { context ->
                                    teleportHere(
                                        context,
                                        StringArgumentType.getString(context, "target"),
                                        teleports,
                                        teleportSettings,
                                        kernel,
                                    )
                                }
                    )
        )
        dispatcher.register(
            Commands.literal("tppos")
                    .requires { it.requiresPermission(permissions, CommandPermissions.TP_POS) }
                    .then(
                        Commands.argument("x", DoubleArgumentType.doubleArg())
                                .then(
                                    Commands.argument("y", DoubleArgumentType.doubleArg())
                                            .then(
                                                Commands.argument(
                                                    "z",
                                                    DoubleArgumentType.doubleArg()
                                                )
                                                        .executes { context ->
                                                            teleportPos(
                                                                context,
                                                                null,
                                                                teleports,
                                                                teleportSettings,
                                                                kernel,
                                                            )
                                                        }
                                                        .then(
                                                            Commands.argument(
                                                                "dimension",
                                                                StringArgumentType.word()
                                                            ).executes { context ->
                                                                teleportPos(
                                                                    context,
                                                                    StringArgumentType.getString(
                                                                        context,
                                                                        "dimension"
                                                                    ),
                                                                    teleports,
                                                                    teleportSettings,
                                                                    kernel,
                                                                )
                                                            }
                                                        )
                                            )
                                )
                    )
        )
    }

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
                else ->
                    when (
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

                        else -> TeleportFeedback.failure(outcome) ?: return@launch
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
        permissions: PermissionService,
    ): Int {
        val source = context.source
        if (secondName == null) {
            val subject = source.player
                ?: return source.replyError(Messages.prefixed("this command requires a player"))
            val target = PlayerResolver.onlineByName(source.server, firstName)
                ?: return source.replyError(Messages.prefixed("player '$firstName' is not online"))

            return if (subject.uuid == target.uuid) {
                source.replyError(Messages.prefixed("you are already there"))
            } else launch(
                source,
                subject.uuid,
                target.uuid,
                TeleportCause.DIRECT,
                teleports,
                teleportSettings,
                kernel
            )
        }

        if (!source.requiresPermission(permissions, CommandPermissions.TP)) {
            return source.replyError(Messages.prefixed("no permission"))
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
            x = DoubleArgumentType.getDouble(context, "x"),
            y = DoubleArgumentType.getDouble(context, "y"),
            z = DoubleArgumentType.getDouble(context, "z"),
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
