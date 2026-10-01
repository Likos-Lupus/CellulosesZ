package top.likoslupus.cellulosesz.command

import net.minecraft.commands.CommandSourceStack
import net.minecraft.network.chat.Component
import top.likoslupus.cellulosesz.player.PlayerResolver
import top.likoslupus.cellulosesz.runtime.RuntimeKernel
import java.util.*

internal fun CommandSourceStack.reply(message: Component): Int {
    sendSuccess({ message }, false)
    return 1
}

internal fun CommandSourceStack.replyError(message: Component): Int {
    sendFailure(message)
    return 0
}

internal suspend fun RuntimeKernel.messagePlayer(playerId: UUID, message: Component) {
    onServerThread {
        PlayerResolver.onlineById(requireServer(), playerId)?.sendSystemMessage(message)
    }
}
