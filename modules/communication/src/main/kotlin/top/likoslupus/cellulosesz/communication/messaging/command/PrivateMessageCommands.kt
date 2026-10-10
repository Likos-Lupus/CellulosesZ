package top.likoslupus.cellulosesz.communication.messaging.command

import com.mojang.brigadier.arguments.StringArgumentType
import com.mojang.brigadier.context.CommandContext
import net.minecraft.commands.CommandSourceStack
import top.likoslupus.cellulosesz.communication.format.CommunicationMessages
import top.likoslupus.cellulosesz.communication.messaging.PrivateMessageOutcome
import top.likoslupus.cellulosesz.communication.messaging.PrivateMessageService
import top.likoslupus.cellulosesz.core.command.CommandCategory
import top.likoslupus.cellulosesz.core.command.dsl.CommandDefinition
import top.likoslupus.cellulosesz.core.command.dsl.command
import top.likoslupus.cellulosesz.core.command.dsl.greedyString
import top.likoslupus.cellulosesz.core.command.dsl.word
import top.likoslupus.cellulosesz.core.command.reply
import top.likoslupus.cellulosesz.core.command.replyError
import top.likoslupus.cellulosesz.core.permission.CommandPermissions

/** `/msg` and `/reply` (alias `/r`). Synchronous: no filesystem or suspension on the hot path. */
internal object PrivateMessageCommands {

    fun commands(messages: PrivateMessageService): List<CommandDefinition> = listOf(
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
                            messages,
                            get(player)
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
                        messages
                    )
                }
            }
        },
    )

    private fun send(
        context: CommandContext<CommandSourceStack>,
        messages: PrivateMessageService,
        targetName: String,
    ): Int {
        val source = context.source
        val senderId = source.player?.uuid
            ?: return source.replyError(CommunicationMessages.requiresPlayer())
        val text = StringArgumentType.getString(context, "message")
        return source.respond(
            messages.send(
                senderId,
                targetName,
                text
            )
        )
    }

    private fun reply(
        context: CommandContext<CommandSourceStack>,
        messages: PrivateMessageService,
    ): Int {
        val source = context.source
        val senderId = source.player?.uuid
            ?: return source.replyError(CommunicationMessages.requiresPlayer())
        val text = StringArgumentType.getString(context, "message")
        return source.respond(messages.reply(senderId, text))
    }

    private fun CommandSourceStack.respond(outcome: PrivateMessageOutcome): Int =
        when (outcome) {
            is PrivateMessageOutcome.Delivered ->
                reply(CommunicationMessages.privateMessageFeedback(outcome))

            else -> replyError(CommunicationMessages.privateMessageFeedback(outcome))
        }

}
