package top.likoslupus.cellulosesz.administration.vanish

import com.mojang.brigadier.CommandDispatcher
import com.mojang.brigadier.context.CommandContext
import net.minecraft.commands.CommandSourceStack
import net.minecraft.commands.Commands
import net.minecraft.network.chat.Component
import top.likoslupus.cellulosesz.administration.moderation.PlayerIdentity
import top.likoslupus.cellulosesz.administration.moderation.command.moderationLaunch
import top.likoslupus.cellulosesz.administration.moderation.moderationActor
import top.likoslupus.cellulosesz.core.command.canUseModeratorCommands
import top.likoslupus.cellulosesz.core.command.replyError
import top.likoslupus.cellulosesz.core.runtime.RuntimeKernel
import top.likoslupus.cellulosesz.core.text.Messages

/** `/vanish [on|off]` — self-only, session-only. */
internal object VanishCommands {

    fun register(
        dispatcher: CommandDispatcher<CommandSourceStack>,
        service: VanishService,
        kernel: RuntimeKernel,
    ) {
        dispatcher.register(
            Commands.literal("vanish")
                    .requires { it.canUseModeratorCommands() }
                    .executes { context -> apply(context, null, service, kernel) }
                    .then(
                        Commands.literal("on")
                                .executes { context -> apply(context, true, service, kernel) }
                    )
                    .then(
                        Commands.literal("off")
                                .executes { context -> apply(context, false, service, kernel) }
                    )
        )
    }

    private fun apply(
        context: CommandContext<CommandSourceStack>,
        enable: Boolean?,
        service: VanishService,
        kernel: RuntimeKernel,
    ): Int {
        val source = context.source
        val player = source.player
            ?: return source.replyError(Messages.prefixed("this command requires a player"))
        val desired = enable
            ?: !service.isVanished(player.uuid)
        val actor = source.moderationActor()
        val identity = PlayerIdentity(player.uuid, player.gameProfile.name)

        return moderationLaunch(
            source,
            kernel,
            "updating vanish..."
        ) {
            feedback(
                service.set(
                    actor,
                    identity,
                    desired
                )
            )
        }
    }

    private fun feedback(result: VanishResult): Component {
        val state = if (result.enabled) "enabled" else "disabled"
        val note = if (result.auditRecorded) "" else " (audit record could not be written)"
        return Messages.prefixed("vanish $state$note")
    }

}
