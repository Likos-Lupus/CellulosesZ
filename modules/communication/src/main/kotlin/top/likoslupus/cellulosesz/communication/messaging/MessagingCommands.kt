package top.likoslupus.cellulosesz.communication.messaging

import com.mojang.brigadier.CommandDispatcher
import com.mojang.brigadier.arguments.StringArgumentType
import com.mojang.brigadier.context.CommandContext
import net.minecraft.commands.CommandSourceStack
import net.minecraft.commands.Commands
import top.likoslupus.cellulosesz.communication.PrivateMessageObservation
import top.likoslupus.cellulosesz.core.command.reply
import top.likoslupus.cellulosesz.core.command.replyError
import top.likoslupus.cellulosesz.core.player.PlayerResolver
import top.likoslupus.cellulosesz.core.text.Messages
import java.util.*

internal object MessagingCommands {

    fun register(
        dispatcher: CommandDispatcher<CommandSourceStack>,
        conversation: ConversationState,
        enabled: () -> Boolean,
        canSend: (UUID) -> Boolean,
        observe: (PrivateMessageObservation) -> Unit,
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
                                                    StringArgumentType.getString(context, "player"),
                                                    StringArgumentType.getString(
                                                        context,
                                                        "message"
                                                    ),
                                                    conversation,
                                                    enabled,
                                                    canSend,
                                                    observe,
                                                )
                                            }
                                )
                    )
        )
        dispatcher.register(
            Commands.literal("reply")
                    .then(
                        Commands.argument("message", StringArgumentType.greedyString())
                                .executes { context ->
                                    reply(
                                        context,
                                        StringArgumentType.getString(context, "message"),
                                        conversation,
                                        enabled,
                                        canSend,
                                        observe,
                                    )
                                }
                    )
        )
    }

    private fun send(
        context: CommandContext<CommandSourceStack>,
        targetName: String,
        text: String,
        conversation: ConversationState,
        enabled: () -> Boolean,
        canSend: (UUID) -> Boolean,
        observe: (PrivateMessageObservation) -> Unit,
    ): Int {
        val source = context.source
        if (!enabled()) {
            return source.replyError(Messages.prefixed("messaging is disabled"))
        }

        val sender = source.player
            ?: return source.replyError(Messages.prefixed("this command requires a player"))
        if (!canSend(sender.uuid)) {
            return source.replyError(Messages.prefixed("you are muted"))
        }

        val target = PlayerResolver.onlineByName(source.server, targetName)
            ?: return source.replyError(Messages.prefixed("player '$targetName' is not online"))
        if (target.uuid == sender.uuid) {
            return source.replyError(Messages.prefixed("you cannot message yourself"))
        }

        conversation.record(sender.uuid, target.uuid)
        target.sendSystemMessage(
            Messages.privateMessage(
                sender.name.string,
                text
            )
        )
        observe(
            PrivateMessageObservation(
                senderId = sender.uuid,
                senderName = sender.name.string,
                targetId = target.uuid,
                targetName = target.name.string,
                text = text,
            )
        )
        return source.reply(
            Messages.privateMessageSent(
                target.name.string,
                text
            )
        )
    }

    private fun reply(
        context: CommandContext<CommandSourceStack>,
        text: String,
        conversation: ConversationState,
        enabled: () -> Boolean,
        canSend: (UUID) -> Boolean,
        observe: (PrivateMessageObservation) -> Unit,
    ): Int {
        val source = context.source
        if (!enabled()) {
            return source.replyError(Messages.prefixed("messaging is disabled"))
        }

        val sender = source.player
            ?: return source.replyError(Messages.prefixed("this command requires a player"))
        if (!canSend(sender.uuid)) {
            return source.replyError(Messages.prefixed("you are muted"))
        }

        val partnerId = conversation.partnerOf(sender.uuid)
            ?: return source.replyError(Messages.prefixed("you have no one to reply to"))
        val target = PlayerResolver.onlineById(source.server, partnerId)
            ?: return source.replyError(Messages.prefixed("that player is not online"))

        conversation.record(sender.uuid, target.uuid)
        target.sendSystemMessage(
            Messages.privateMessage(
                sender.name.string,
                text
            )
        )
        observe(
            PrivateMessageObservation(
                senderId = sender.uuid,
                senderName = sender.name.string,
                targetId = target.uuid,
                targetName = target.name.string,
                text = text,
            )
        )

        return source.reply(
            Messages.privateMessageSent(
                target.name.string,
                text
            )
        )
    }

}
