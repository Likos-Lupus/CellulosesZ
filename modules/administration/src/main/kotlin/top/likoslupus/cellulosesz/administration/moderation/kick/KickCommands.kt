package top.likoslupus.cellulosesz.administration.moderation.kick

import com.mojang.brigadier.arguments.StringArgumentType
import com.mojang.brigadier.context.CommandContext
import net.minecraft.commands.CommandSourceStack
import top.likoslupus.cellulosesz.administration.config.ModerationSettings
import top.likoslupus.cellulosesz.administration.moderation.command.*
import top.likoslupus.cellulosesz.administration.moderation.moderationActor
import top.likoslupus.cellulosesz.administration.moderation.notify.ModerationNotifier
import top.likoslupus.cellulosesz.core.command.CommandCategory
import top.likoslupus.cellulosesz.core.command.dsl.CommandDefinition
import top.likoslupus.cellulosesz.core.command.dsl.command
import top.likoslupus.cellulosesz.core.command.dsl.greedyString
import top.likoslupus.cellulosesz.core.command.dsl.word
import top.likoslupus.cellulosesz.core.permission.CommandPermissions
import top.likoslupus.cellulosesz.core.runtime.RuntimeKernel

/** `/kick` and `/kickall`. */
internal object KickCommands {

    fun commands(
        kick: KickService,
        notifier: ModerationNotifier,
        settings: () -> ModerationSettings,
        kernel: RuntimeKernel,
    ): List<CommandDefinition> = listOf(
        command(
            name = "kick",
            category = CommandCategory.ADMINISTRATION,
            permission = CommandPermissions.KICK,
            documentation = "administration/kick",
        ) {
            argument("player", word()) {
                executes {
                    kick(
                        context,
                        null,
                        kick,
                        notifier,
                        settings,
                        kernel
                    )
                }
                argument("reason", greedyString()) { reason ->
                    executes {
                        kick(
                            context,
                            get(reason),
                            kick,
                            notifier,
                            settings,
                            kernel
                        )
                    }
                }
            }
        },

        command(
            name = "kickall",
            category = CommandCategory.ADMINISTRATION,
            permission = CommandPermissions.KICK_ALL,
            documentation = "administration/kickall",
        ) {
            executes {
                kickAll(
                    context,
                    null,
                    kick,
                    notifier,
                    settings,
                    kernel
                )
            }
            argument("reason", greedyString()) { reason ->
                executes {
                    kickAll(
                        context,
                        get(reason),
                        kick,
                        notifier,
                        settings,
                        kernel
                    )
                }
            }
        },
    )

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
