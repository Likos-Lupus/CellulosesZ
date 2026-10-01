package top.likoslupus.cellulosesz.communication

import com.mojang.brigadier.CommandDispatcher
import net.minecraft.commands.CommandSourceStack
import top.likoslupus.cellulosesz.communication.messaging.ConversationState
import top.likoslupus.cellulosesz.communication.messaging.MessagingCommands
import java.util.*

/**
 * Public surface of the communication bounded context. Internals stay `internal`.
 */
public class CommunicationFeature internal constructor(
    private val conversation: ConversationState,
) {

    public fun registerCommands(
        dispatcher: CommandDispatcher<CommandSourceStack>,
        enabled: () -> Boolean,
    ) {
        MessagingCommands.register(dispatcher, conversation, enabled)
    }

    public fun onPlayerQuit(playerId: UUID) {
        conversation.clear(playerId)
    }

}

public fun createCommunicationFeature(): CommunicationFeature =
    CommunicationFeature(ConversationState())
