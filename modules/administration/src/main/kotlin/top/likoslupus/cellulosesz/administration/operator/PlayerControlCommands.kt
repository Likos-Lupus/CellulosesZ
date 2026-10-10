package top.likoslupus.cellulosesz.administration.operator

import com.mojang.brigadier.arguments.StringArgumentType
import com.mojang.brigadier.context.CommandContext
import net.minecraft.commands.CommandSourceStack
import top.likoslupus.cellulosesz.administration.moderation.command.moderationLaunch
import top.likoslupus.cellulosesz.administration.moderation.moderationActor
import top.likoslupus.cellulosesz.core.command.CommandCategory
import top.likoslupus.cellulosesz.core.command.dsl.*
import top.likoslupus.cellulosesz.core.command.replyError
import top.likoslupus.cellulosesz.core.permission.CommandPermissions
import top.likoslupus.cellulosesz.core.runtime.RuntimeKernel
import top.likoslupus.cellulosesz.core.text.Messages

/** `/kill`, `/gamemode` and console-only `/sudo`. */
internal object PlayerControlCommands {

    private const val MAX_SUDO_COMMAND_LENGTH: Int = 256

    fun commands(
        service: PlayerControlService,
        kernel: RuntimeKernel,
    ): List<CommandDefinition> = listOf(
        command(
            name = "kill",
            category = CommandCategory.ADMINISTRATION,
            permission = CommandPermissions.KILL,
            documentation = "administration/kill",
        ) {
            argument("player", word()) { player ->
                executes {
                    val target = get(player)
                    val source = context.source
                    val actor = source.moderationActor()
                    moderationLaunch(source, kernel, "killing $target...") {
                        PlayerControlFeedback.kill(
                            service.kill(
                                actor,
                                target
                            )
                        )
                    }
                }
            }
        },

        command(
            name = "gamemode",
            category = CommandCategory.ADMINISTRATION,
            permission = CommandPermissions.GAMEMODE,
            documentation = "administration/gamemode",
        ) {
            argument("mode", word()) {
                executes {
                    gameMode(
                        context,
                        null,
                        service,
                        kernel
                    )
                }
                argument("player", word()) { player ->
                    executes { gameMode(context, get(player), service, kernel) }
                }
            }
        },

        command(
            name = "sudo",
            category = CommandCategory.ADMINISTRATION,
            permission = CommandPermissions.SUDO,
            documentation = "administration/sudo",
            sourceAccess = SourceAccess.NON_PLAYER,
        ) {
            argument("player", word()) {
                argument("command", greedyString()) {
                    executes { sudo(context, service, kernel) }
                }
            }
        },
    )

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
        val rawCommand = StringArgumentType.getString(
            context,
            "command"
        ).trim()
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
