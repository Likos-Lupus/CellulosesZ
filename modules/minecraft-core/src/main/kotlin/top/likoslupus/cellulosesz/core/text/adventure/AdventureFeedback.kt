package top.likoslupus.cellulosesz.core.text.adventure

import net.kyori.adventure.text.Component as AdventureComponent
import net.minecraft.commands.CommandSourceStack
import top.likoslupus.cellulosesz.core.command.CommandFeedbackTarget
import top.likoslupus.cellulosesz.core.runtime.RuntimeKernel

/**
 * Adventure-specific feedback helpers. They coexist with the legacy vanilla-component helpers and
 * preserve the server-thread rule: a captured [CommandFeedbackTarget] is re-resolved on the server
 * thread, never a retained [net.minecraft.server.level.ServerPlayer].
 */
public suspend fun RuntimeKernel.message(
    target: CommandFeedbackTarget,
    adventure: AdventureRuntime,
    message: AdventureComponent,
) {
    onServerThread {
        adventure.audience(target).sendMessage(message)
    }
}

/** Synchronous Adventure reply for non-suspending command paths. Returns the Brigadier result. */
public fun CommandSourceStack.reply(
    adventure: AdventureRuntime,
    message: AdventureComponent,
): Int {
    adventure.requireAudiences().audience(this).sendMessage(message)
    return 1
}

/** Synchronous Adventure failure reply; Adventure does not preserve vanilla failure slots. */
public fun CommandSourceStack.replyError(
    adventure: AdventureRuntime,
    message: AdventureComponent,
): Int {
    adventure.requireAudiences().audience(this).sendMessage(message)
    return 0
}
