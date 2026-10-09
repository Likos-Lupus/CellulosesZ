package top.likoslupus.cellulosesz.administration.socialspy

import com.mojang.brigadier.CommandDispatcher
import net.minecraft.commands.CommandSourceStack
import net.minecraft.commands.Commands
import net.minecraft.network.chat.Component
import top.likoslupus.cellulosesz.administration.moderation.PlayerIdentity
import top.likoslupus.cellulosesz.administration.moderation.command.ModerationFeedback
import top.likoslupus.cellulosesz.administration.moderation.command.moderationLaunch
import top.likoslupus.cellulosesz.administration.moderation.moderationActor
import top.likoslupus.cellulosesz.core.command.replyError
import top.likoslupus.cellulosesz.core.command.requiresPermission
import top.likoslupus.cellulosesz.core.permission.CommandPermissions
import top.likoslupus.cellulosesz.core.permission.PermissionService
import top.likoslupus.cellulosesz.core.runtime.RuntimeKernel
import top.likoslupus.cellulosesz.core.text.Messages

/** `/socialspy [on|off]` — session-only self toggle. */
internal object SocialSpyCommands {

    fun register(
        dispatcher: CommandDispatcher<CommandSourceStack>,
        service: SocialSpyService,
        kernel: RuntimeKernel,
        permissions: PermissionService,
    ) {
        dispatcher.register(
            Commands.literal("socialspy")
                    .requires { it.requiresPermission(permissions, CommandPermissions.SOCIAL_SPY) }
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
        context: com.mojang.brigadier.context.CommandContext<CommandSourceStack>,
        enable: Boolean?,
        service: SocialSpyService,
        kernel: RuntimeKernel,
    ): Int {
        val source = context.source
        val player = source.player
            ?: return source.replyError(Messages.prefixed("this command requires a player"))
        val desired = enable ?: !service.isSpying(player.uuid)
        val actor = source.moderationActor()
        val identity = PlayerIdentity(player.uuid, player.gameProfile.name)

        return moderationLaunch(
            source,
            kernel,
            "updating social spy..."
        ) {
            val result = service.set(actor, identity, desired)
            feedback(result)
        }
    }

    private fun feedback(result: SocialSpyResult): Component {
        val state = if (result.enabled) "enabled" else "disabled"
        val note = ModerationFeedback.auditNote(result.auditRecorded)
        return Messages.prefixed("social spy $state$note")
    }

}
