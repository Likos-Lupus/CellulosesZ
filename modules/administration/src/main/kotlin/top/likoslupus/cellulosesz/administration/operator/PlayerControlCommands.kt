package top.likoslupus.cellulosesz.administration.operator

import com.mojang.brigadier.arguments.StringArgumentType
import com.mojang.brigadier.context.CommandContext
import net.minecraft.commands.CommandSourceStack
import top.likoslupus.cellulosesz.administration.format.*
import top.likoslupus.cellulosesz.administration.moderation.command.moderationLaunch
import top.likoslupus.cellulosesz.administration.moderation.moderationActor
import top.likoslupus.cellulosesz.core.command.CommandCategory
import top.likoslupus.cellulosesz.core.command.dsl.*
import top.likoslupus.cellulosesz.core.permission.CommandPermissions
import top.likoslupus.cellulosesz.core.runtime.RuntimeKernel
import top.likoslupus.cellulosesz.core.text.MessageKey
import top.likoslupus.cellulosesz.core.text.adventure.AdventureRuntime
import top.likoslupus.cellulosesz.core.text.adventure.replyError
import top.likoslupus.cellulosesz.core.text.i18n.LocalizedMessages
import top.likoslupus.cellulosesz.core.text.model.LocalizedText
import top.likoslupus.cellulosesz.core.text.presentation.TextPresentation
import top.likoslupus.cellulosesz.core.text.presentation.sendNotices

/** `/kill`, `/gamemode` and console-only `/sudo`. */
internal object PlayerControlCommands {

    private const val MAX_SUDO_COMMAND_LENGTH: Int = 256

    fun commands(
        service: PlayerControlService,
        kernel: RuntimeKernel,
        messages: LocalizedMessages,
        adventure: AdventureRuntime,
        presentation: TextPresentation,
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
                    val playerId = source.player?.uuid
                    moderationLaunch(
                        source,
                        kernel,
                        messages,
                        adventure,
                        presentation,
                        AdministrationMessages.pendingKill(target),
                    ) { feedback ->
                        kernel.sendNotices(
                            feedback,
                            adventure,
                            presentation,
                            playerId,
                            listOf(AdministrationMessages.kill(service.kill(actor, target))),
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
                        kernel,
                        messages,
                        adventure,
                        presentation
                    )
                }
                argument("player", word()) { player ->
                    executes {
                        gameMode(
                            context,
                            get(player),
                            service,
                            kernel,
                            messages,
                            adventure,
                            presentation
                        )
                    }
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
                    executes {
                        sudo(
                            context,
                            service,
                            kernel,
                            messages,
                            adventure,
                            presentation
                        )
                    }
                }
            }
        },
    )

    private fun gameMode(
        context: CommandContext<CommandSourceStack>,
        target: String?,
        service: PlayerControlService,
        kernel: RuntimeKernel,
        messages: LocalizedMessages,
        adventure: AdventureRuntime,
        presentation: TextPresentation,
    ): Int {
        val source = context.source
        val playerId = source.player?.uuid
        val rawMode = StringArgumentType.getString(context, "mode")
        val mode = PlayerGameMode.parse(rawMode)
            ?: return source.replyError(
                adventure,
                messages.renderFor(playerId, MessageKey("administration.gamemode.invalid")),
            )
        val actor = source.moderationActor()
        return moderationLaunch(
            source,
            kernel,
            messages,
            adventure,
            presentation,
            LocalizedText(MessageKey("administration.pending.gamemode")),
        ) { feedback ->
            kernel.sendNotices(
                feedback,
                adventure,
                presentation,
                playerId,
                listOf(
                    AdministrationMessages.gameMode(
                        service.setGameMode(
                            actor,
                            target,
                            mode
                        )
                    )
                ),
            )
        }
    }

    private fun sudo(
        context: CommandContext<CommandSourceStack>,
        service: PlayerControlService,
        kernel: RuntimeKernel,
        messages: LocalizedMessages,
        adventure: AdventureRuntime,
        presentation: TextPresentation,
    ): Int {
        val source = context.source
        val playerId = source.player?.uuid
        val target = StringArgumentType.getString(context, "player")
        val rawCommand = StringArgumentType.getString(
            context,
            "command"
        ).trim()
        val command = rawCommand.removePrefix("/")
        val actor = source.moderationActor()

        return when {
            command.isBlank() ->
                source.replyError(
                    adventure,
                    messages.renderFor(playerId, MessageKey("administration.sudo.blank")),
                )

            command.length > MAX_SUDO_COMMAND_LENGTH ->
                source.replyError(
                    adventure,
                    messages.renderFor(playerId, MessageKey("administration.sudo.too_long")),
                )

            command.any(Char::isISOControl) ->
                source.replyError(
                    adventure,
                    messages.renderFor(playerId, MessageKey("administration.sudo.control_chars")),
                )

            else -> moderationLaunch(
                source,
                kernel,
                messages,
                adventure,
                presentation,
                AdministrationMessages.pendingSudo(target),
            ) { feedback ->
                kernel.sendNotices(
                    feedback,
                    adventure,
                    presentation,
                    playerId,
                    listOf(
                        AdministrationMessages.sudo(
                            service.executeAsPlayer(
                                actor,
                                target,
                                command
                            )
                        )
                    ),
                )
            }
        }
    }

}
