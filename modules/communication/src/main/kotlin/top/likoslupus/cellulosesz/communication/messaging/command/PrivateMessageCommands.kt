package top.likoslupus.cellulosesz.communication.messaging.command

import com.mojang.brigadier.arguments.StringArgumentType
import com.mojang.brigadier.context.CommandContext
import net.minecraft.commands.CommandSourceStack
import top.likoslupus.cellulosesz.communication.command.replyNoticeOutcome
import top.likoslupus.cellulosesz.communication.format.CommunicationMessages
import top.likoslupus.cellulosesz.communication.messaging.PrivateMessageOutcome
import top.likoslupus.cellulosesz.communication.messaging.PrivateMessageService
import top.likoslupus.cellulosesz.core.command.CommandCategory
import top.likoslupus.cellulosesz.core.command.dsl.CommandDefinition
import top.likoslupus.cellulosesz.core.command.dsl.command
import top.likoslupus.cellulosesz.core.command.dsl.greedyString
import top.likoslupus.cellulosesz.core.command.dsl.word
import top.likoslupus.cellulosesz.core.permission.CommandPermissions
import top.likoslupus.cellulosesz.core.text.MessageKey
import top.likoslupus.cellulosesz.core.text.adventure.AdventureRuntime
import top.likoslupus.cellulosesz.core.text.adventure.replyError
import top.likoslupus.cellulosesz.core.text.i18n.LocalizedMessages
import top.likoslupus.cellulosesz.core.text.presentation.TextPresentation

/** `/msg` and `/reply` (alias `/r`). Synchronous: no filesystem or suspension on the hot path. */
internal object PrivateMessageCommands {

    fun commands(
        service: PrivateMessageService,
        messages: LocalizedMessages,
        adventure: AdventureRuntime,
        presentation: TextPresentation,
    ): List<CommandDefinition> = listOf(
        command(
            name = "msg",
            category = CommandCategory.COMMUNICATION,
            permission = CommandPermissions.MSG,
            documentation = "communication/msg",
        ) {
            argument("player", word()) { player ->
                argument("message", greedyString()) {
                    executesPlayer {
                        send(
                            context,
                            service,
                            messages,
                            adventure,
                            presentation,
                            get(player),
                        )
                    }
                }
            }
        },

        command(
            name = "reply",
            category = CommandCategory.COMMUNICATION,
            permission = CommandPermissions.REPLY,
            documentation = "communication/reply",
            aliases = setOf("r"),
        ) {
            argument("message", greedyString()) {
                executesPlayer {
                    reply(
                        context,
                        service,
                        messages,
                        adventure,
                        presentation,
                    )
                }
            }
        },
    )

    private fun send(
        context: CommandContext<CommandSourceStack>,
        service: PrivateMessageService,
        messages: LocalizedMessages,
        adventure: AdventureRuntime,
        presentation: TextPresentation,
        targetName: String,
    ): Int {
        val source = context.source
        val senderId = source.player?.uuid
            ?: return source.replyError(
                adventure,
                messages.renderFor(
                    null,
                    MessageKey("core.error.player_required")
                )
            )
        val text = StringArgumentType.getString(context, "message")
        val outcome = service.send(
            senderId,
            targetName,
            text,
        )
        return source.replyNoticeOutcome(
            adventure,
            presentation,
            senderId,
            CommunicationMessages.privateMessageFeedback(outcome),
            outcome is PrivateMessageOutcome.Delivered,
        )
    }

    private fun reply(
        context: CommandContext<CommandSourceStack>,
        service: PrivateMessageService,
        messages: LocalizedMessages,
        adventure: AdventureRuntime,
        presentation: TextPresentation,
    ): Int {
        val source = context.source
        val senderId = source.player?.uuid
            ?: return source.replyError(
                adventure,
                messages.renderFor(
                    null,
                    MessageKey("core.error.player_required")
                )
            )
        val text = StringArgumentType.getString(context, "message")
        val outcome = service.reply(senderId, text)
        return source.replyNoticeOutcome(
            adventure,
            presentation,
            senderId,
            CommunicationMessages.privateMessageFeedback(outcome),
            outcome is PrivateMessageOutcome.Delivered,
        )
    }

}
