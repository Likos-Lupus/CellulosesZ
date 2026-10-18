package top.likoslupus.cellulosesz.administration.moderation.kick

import com.mojang.brigadier.arguments.StringArgumentType
import com.mojang.brigadier.context.CommandContext
import net.minecraft.commands.CommandSourceStack
import top.likoslupus.cellulosesz.administration.config.ModerationSettings
import top.likoslupus.cellulosesz.administration.format.*
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
import top.likoslupus.cellulosesz.core.text.MessageKey
import top.likoslupus.cellulosesz.core.text.adventure.AdventureRuntime
import top.likoslupus.cellulosesz.core.text.i18n.LocalizedMessages
import top.likoslupus.cellulosesz.core.text.model.LocalizedText
import top.likoslupus.cellulosesz.core.text.presentation.TextPresentation
import top.likoslupus.cellulosesz.core.text.presentation.sendNotices

/** `/kick` and `/kickall`. */
internal object KickCommands {

    fun commands(
        kick: KickService,
        notifier: ModerationNotifier,
        settings: () -> ModerationSettings,
        kernel: RuntimeKernel,
        messages: LocalizedMessages,
        adventure: AdventureRuntime,
        presentation: TextPresentation,
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
                        kernel,
                        messages,
                        adventure,
                        presentation
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
                            kernel,
                            messages,
                            adventure,
                            presentation
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
                    kernel,
                    messages,
                    adventure,
                    presentation
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
                        kernel,
                        messages,
                        adventure,
                        presentation
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
        messages: LocalizedMessages,
        adventure: AdventureRuntime,
        presentation: TextPresentation,
    ): Int {
        val source = context.source
        val config = settings()
        val reason = resolveReason(
            rawReason,
            config.defaultKickReason,
            config.maxReasonLength
        ) ?: return invalidReason(source, messages, adventure)
        val target = StringArgumentType.getString(context, "player")
        val actor = source.moderationActor()
        val playerId = source.player?.uuid

        return moderationLaunch(
            source,
            kernel,
            messages,
            adventure,
            presentation,
            AdministrationMessages.pendingKick(target),
        ) { feedback ->
            val result = kick.kick(actor, target, reason)
            if (result is KickResult.Kicked
                && config.notifyModerators
            ) {
                notifyModerators(
                    kernel,
                    notifier,
                    AdministrationMessages.notifyKick(actor.displayName, result.target.name)
                )
            }
            kernel.sendNotices(
                feedback,
                adventure,
                presentation,
                playerId,
                listOf(AdministrationMessages.kick(result)),
            )
        }
    }

    private fun kickAll(
        context: CommandContext<CommandSourceStack>,
        rawReason: String?,
        kick: KickService,
        notifier: ModerationNotifier,
        settings: () -> ModerationSettings,
        kernel: RuntimeKernel,
        messages: LocalizedMessages,
        adventure: AdventureRuntime,
        presentation: TextPresentation,
    ): Int {
        val source = context.source
        val config = settings()
        val reason = resolveReason(
            rawReason,
            config.defaultKickReason,
            config.maxReasonLength
        ) ?: return invalidReason(source, messages, adventure)
        val actor = source.moderationActor()
        val playerId = source.player?.uuid

        return moderationLaunch(
            source,
            kernel,
            messages,
            adventure,
            presentation,
            LocalizedText(MessageKey("administration.pending.kick_all")),
        ) { feedback ->
            val result = kick.kickAll(actor, reason)
            if (config.notifyModerators) {
                notifyModerators(
                    kernel,
                    notifier,
                    AdministrationMessages.notifyKickAll(actor.displayName, result.kicked)
                )
            }
            kernel.sendNotices(
                feedback,
                adventure,
                presentation,
                playerId,
                AdministrationMessages.kickAll(result),
            )
        }
    }

}
