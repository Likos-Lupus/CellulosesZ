package top.likoslupus.cellulosesz.administration.moderation.kick

import com.mojang.brigadier.CommandDispatcher
import com.mojang.brigadier.arguments.StringArgumentType
import com.mojang.brigadier.context.CommandContext
import net.minecraft.commands.CommandSourceStack
import net.minecraft.commands.Commands
import top.likoslupus.cellulosesz.administration.config.ModerationSettings
import top.likoslupus.cellulosesz.administration.moderation.command.*
import top.likoslupus.cellulosesz.administration.moderation.moderationActor
import top.likoslupus.cellulosesz.administration.moderation.notify.ModerationNotifier
import top.likoslupus.cellulosesz.core.command.canUseModeratorCommands
import top.likoslupus.cellulosesz.core.runtime.RuntimeKernel

/** `/kick` and `/kickall`. */
internal object KickCommands {

    fun register(
        dispatcher: CommandDispatcher<CommandSourceStack>,
        kick: KickService,
        notifier: ModerationNotifier,
        settings: () -> ModerationSettings,
        kernel: RuntimeKernel,
    ) {
        dispatcher.register(
            Commands.literal("kick")
                    .requires { it.canUseModeratorCommands() }
                    .then(
                        Commands.argument("player", StringArgumentType.word())
                                .executes { context ->
                                    kick(
                                        context,
                                        null,
                                        kick,
                                        notifier,
                                        settings,
                                        kernel
                                    )
                                }
                                .then(
                                    Commands.argument("reason", StringArgumentType.greedyString())
                                            .executes { context ->
                                                kick(
                                                    context,
                                                    StringArgumentType.getString(context, "reason"),
                                                    kick,
                                                    notifier,
                                                    settings,
                                                    kernel,
                                                )
                                            }
                                )
                    )
        )
        dispatcher.register(
            Commands.literal("kickall")
                    .requires { it.canUseModeratorCommands() }
                    .executes { context ->
                        kickAll(
                            context,
                            null,
                            kick,
                            notifier,
                            settings,
                            kernel
                        )
                    }
                    .then(
                        Commands.argument("reason", StringArgumentType.greedyString())
                                .executes { context ->
                                    kickAll(
                                        context,
                                        StringArgumentType.getString(context, "reason"),
                                        kick,
                                        notifier,
                                        settings,
                                        kernel,
                                    )
                                }
                    )
        )
    }

    private fun kick(
        context: CommandContext<CommandSourceStack>,
        rawReason: String?,
        kick: KickService,
        notifier: ModerationNotifier,
        settings: () -> ModerationSettings,
        kernel: RuntimeKernel,
    ): Int {
        val source = context.source
        val config = settings()
        val reason = resolveReason(
            rawReason,
            config.defaultKickReason,
            config.maxReasonLength
        ) ?: return invalidReason(source)
        val target = StringArgumentType.getString(context, "player")
        val actor = source.moderationActor()

        return moderationLaunch(
            source,
            kernel,
            "kicking $target..."
        ) {
            val result = kick.kick(actor, target, reason)
            if (result is KickResult.Kicked
                && config.notifyModerators
            ) {
                notifyModerators(
                    kernel,
                    notifier,
                    "${actor.displayName} kicked ${result.target.name}"
                )
            }
            ModerationFeedback.kick(result)
        }
    }

    private fun kickAll(
        context: CommandContext<CommandSourceStack>,
        rawReason: String?,
        kick: KickService,
        notifier: ModerationNotifier,
        settings: () -> ModerationSettings,
        kernel: RuntimeKernel,
    ): Int {
        val source = context.source
        val config = settings()
        val reason = resolveReason(
            rawReason,
            config.defaultKickReason,
            config.maxReasonLength
        ) ?: return invalidReason(source)
        val actor = source.moderationActor()

        return moderationLaunch(
            source,
            kernel,
            "kicking all players..."
        ) {
            val result = kick.kickAll(actor, reason)
            if (config.notifyModerators) {
                notifyModerators(
                    kernel,
                    notifier,
                    "${actor.displayName} kicked ${result.kicked} player(s)"
                )
            }
            ModerationFeedback.kickAll(result)
        }
    }

}
