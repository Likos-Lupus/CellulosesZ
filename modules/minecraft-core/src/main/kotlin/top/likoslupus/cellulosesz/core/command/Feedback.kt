package top.likoslupus.cellulosesz.core.command

import net.minecraft.commands.CommandSourceStack
import net.minecraft.network.chat.Component
import top.likoslupus.cellulosesz.core.player.PlayerResolver
import top.likoslupus.cellulosesz.core.runtime.RuntimeKernel
import java.util.*

/** Sends a success feedback component and returns the Brigadier success result. */
public fun CommandSourceStack.reply(message: Component): Int {
    sendSuccess({ message }, false)
    return 1
}

/** Sends a failure feedback component and returns the Brigadier failure result. */
public fun CommandSourceStack.replyError(message: Component): Int {
    sendFailure(message)
    return 0
}

/** Delivers a component to an online player on the server thread, if still online. */
public suspend fun RuntimeKernel.messagePlayer(
    playerId: UUID,
    message: Component
) {
    onServerThread {
        PlayerResolver.onlineById(
            requireServer(),
            playerId
        )?.sendSystemMessage(message)
    }
}

/** Delivers a component to the captured feedback destination on the server thread. */
public suspend fun RuntimeKernel.message(
    target: CommandFeedbackTarget,
    message: Component
) {
    when (target) {
        is CommandFeedbackTarget.Player ->
            messagePlayer(target.id, message)

        CommandFeedbackTarget.Server ->
            onServerThread {
                requireServer().sendSystemMessage(message)
            }
    }
}
