package top.likoslupus.cellulosesz.administration.operator

import com.mojang.brigadier.CommandDispatcher
import com.mojang.brigadier.arguments.StringArgumentType
import com.mojang.brigadier.context.CommandContext
import net.minecraft.commands.CommandSourceStack
import net.minecraft.commands.Commands
import top.likoslupus.cellulosesz.administration.moderation.command.moderationLaunch
import top.likoslupus.cellulosesz.administration.moderation.moderationActor
import top.likoslupus.cellulosesz.core.command.replyError
import top.likoslupus.cellulosesz.core.command.requiresPermission
import top.likoslupus.cellulosesz.core.permission.CommandPermissions
import top.likoslupus.cellulosesz.core.permission.PermissionService
import top.likoslupus.cellulosesz.core.runtime.RuntimeKernel
import top.likoslupus.cellulosesz.core.text.Messages

/** `/kill`, `/gamemode` and console-only `/sudo`. */
internal object PlayerControlCommands {

    private const val MAX_SUDO_COMMAND_LENGTH: Int = 256

    fun register(
        dispatcher: CommandDispatcher<CommandSourceStack>,
        service: PlayerControlService,
        kernel: RuntimeKernel,
        permissions: PermissionService,
    ) {
        dispatcher.register(
            Commands.literal("kill")
                    .requires { it.requiresPermission(permissions, CommandPermissions.KILL) }
                    .then(
                        Commands.argument("player", StringArgumentType.word())
                                .executes { context ->
                                    val source = context.source
                                    val target = StringArgumentType.getString(context, "player")
                                    val actor = source.moderationActor()
                                    moderationLaunch(source, kernel, "killing $target...") {
                                        PlayerControlFeedback.kill(service.kill(actor, target))
                                    }
                                }
                    )
        )

        dispatcher.register(
            Commands.literal("gamemode")
                    .requires { it.requiresPermission(permissions, CommandPermissions.GAMEMODE) }
                    .then(
                        Commands.argument("mode", StringArgumentType.word())
                                .executes { context -> gameMode(context, null, service, kernel) }
                                .then(
                                    Commands.argument("player", StringArgumentType.word())
                                            .executes { context ->
                                                gameMode(
                                                    context,
                                                    StringArgumentType.getString(context, "player"),
                                                    service,
                                                    kernel,
                                                )
                                            }
                                )
                    )
        )

        dispatcher.register(
            Commands.literal("sudo")
                    .requires {
                        it.requiresPermission(
                            permissions,
                            CommandPermissions.SUDO
                        ) && it.player == null
                    }
                    .then(
                        Commands.argument("player", StringArgumentType.word())
                                .then(
                                    Commands.argument("command", StringArgumentType.greedyString())
                                            .executes { context -> sudo(context, service, kernel) }
                                )
                    )
        )
    }

    private fun gameMode(
        context: CommandContext<CommandSourceStack>,
        target: String?,
        service: PlayerControlService,
        kernel: RuntimeKernel,
    ): Int {
        val source = context.source
        val rawMode = StringArgumentType.getString(context, "mode")
        val mode = PlayerGameMode.parse(rawMode)
            ?: return source.replyError(
                Messages.prefixed("unknown game mode; use survival, creative, adventure or spectator")
            )
        val actor = source.moderationActor()
        return moderationLaunch(source, kernel, "changing game mode...") {
            PlayerControlFeedback.gameMode(
                service.setGameMode(
                    actor,
                    target,
                    mode
                )
            )
        }
    }

    private fun sudo(
        context: CommandContext<CommandSourceStack>,
        service: PlayerControlService,
        kernel: RuntimeKernel,
    ): Int {
        val source = context.source
        val target = StringArgumentType.getString(context, "player")
        val rawCommand = StringArgumentType.getString(context, "command").trim()
        val command = rawCommand.removePrefix("/")

        return when {
            command.isBlank() ->
                source.replyError(Messages.prefixed("provide a command to execute"))

            command.length > MAX_SUDO_COMMAND_LENGTH ->
                source.replyError(Messages.prefixed("command is too long"))

            command.any(Char::isISOControl) ->
                source.replyError(Messages.prefixed("command contains control characters"))

            else -> moderationLaunch(
                source,
                kernel,
                "executing command as $target..."
            ) {
                PlayerControlFeedback.sudo(
                    service.executeAsPlayer(
                        source.moderationActor(),
                        target,
                        command
                    )
                )
            }
        }
    }

}
