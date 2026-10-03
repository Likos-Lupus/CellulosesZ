package top.likoslupus.cellulosesz.communication.messaging.command

import com.mojang.brigadier.CommandDispatcher
import com.mojang.brigadier.arguments.StringArgumentType
import com.mojang.brigadier.context.CommandContext
import net.minecraft.commands.CommandSourceStack
import net.minecraft.commands.Commands
import top.likoslupus.cellulosesz.communication.format.CommunicationMessages
import top.likoslupus.cellulosesz.communication.messaging.PrivateMessageOutcome
import top.likoslupus.cellulosesz.communication.messaging.PrivateMessageService
import top.likoslupus.cellulosesz.core.command.reply
import top.likoslupus.cellulosesz.core.command.replyError

/** `/msg` and `/reply` (alias `/r`). Synchronous: no filesystem or suspension on the hot path. */
internal object PrivateMessageCommands {

    fun register(
        dispatcher: CommandDispatcher<CommandSourceStack>,
        messages: PrivateMessageService,
    ) {
        dispatcher.register(
            Commands.literal("msg")
                    .then(
                        Commands.argument("player", StringArgumentType.word())
                                .then(
                                    Commands.argument("message", StringArgumentType.greedyString())
                                            .executes { context ->
                                                send(
                                                    context,
                                                    messages,
                                                    StringArgumentType.getString(
                                                        context,
                                                        "player"
                                                    ),
                                                )
                                            }
                                )
                    )
        )

        val replyNode = dispatcher.register(
            Commands.literal("reply")
                    .then(
                        Commands.argument("message", StringArgumentType.greedyString())
                                .executes { context ->
                                    reply(
                                        context,
                                        messages
                                    )
                                }
                    )
        )
        dispatcher.register(
            Commands.literal("r")
                    .redirect(replyNode)
        )
    }

    private fun send(
        context: CommandContext<CommandSourceStack>,
        messages: PrivateMessageService,
        targetName: String,
    ): Int {
        val source = context.source
        val senderId = source.player?.uuid
            ?: return source.replyError(CommunicationMessages.requiresPlayer())
        val text = StringArgumentType.getString(context, "message")
        return source.respond(messages.send(senderId, targetName, text))
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
