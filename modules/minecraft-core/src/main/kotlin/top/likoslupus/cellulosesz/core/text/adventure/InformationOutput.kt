package top.likoslupus.cellulosesz.core.text.adventure

import net.kyori.adventure.audience.Audience
import net.kyori.adventure.text.Component
import net.minecraft.commands.CommandSourceStack
import top.likoslupus.cellulosesz.core.command.CommandFeedbackTarget
import top.likoslupus.cellulosesz.core.runtime.RuntimeKernel
import top.likoslupus.cellulosesz.core.text.presentation.DisplayChannel

/**
 * The single final delivery boundary for Adventure components. It decides channel routing (chat vs
 * action bar) and keeps synchronous command success/failure return codes separate from asynchronous
 * notification delivery. It never decides *what* the text says.
 */
public class InformationOutput(
    private val adventure: AdventureRuntime,
    private val kernel: RuntimeKernel? = null,
) {

    /** Synchronous success reply: delivers every line and reports success. */
    public fun reply(
        source: CommandSourceStack,
        lines: List<Component>,
        channel: DisplayChannel = DisplayChannel.CHAT,
    ): Int {
        val audience = adventure.requireAudiences().audience(source)
        val effective = when (source.player) {
            null -> DisplayChannel.CHAT
            else -> channel
        }
        lines.forEach { send(audience, it, effective) }
        return if (lines.isEmpty()) 0 else 1
    }

    /** Synchronous failure reply. Adventure delivery does not preserve the vanilla failure slot. */
    public fun replyError(
        source: CommandSourceStack,
        lines: List<Component>,
        channel: DisplayChannel = DisplayChannel.CHAT,
    ): Int {
        reply(source, lines, channel)
        return 0
    }

    /** Asynchronous delivery to a captured target, re-resolved on the server thread. */
    public suspend fun deliver(
        target: CommandFeedbackTarget,
        lines: List<Component>,
        channel: DisplayChannel = DisplayChannel.CHAT,
    ) {
        if (lines.isEmpty()) {
            return
        }
        val runtime = kernel ?: error("InformationOutput.deliver requires a RuntimeKernel")
        runtime.onServerThread {
            val audience = adventure.audience(target)
            lines.forEach { send(audience, it, channel) }
        }
    }

    private fun send(
        audience: Audience,
        component: Component,
        channel: DisplayChannel
    ) {
        when (channel) {
            DisplayChannel.ACTION_BAR -> audience.sendActionBar(component)
            else -> audience.sendMessage(component)
        }
    }

}
