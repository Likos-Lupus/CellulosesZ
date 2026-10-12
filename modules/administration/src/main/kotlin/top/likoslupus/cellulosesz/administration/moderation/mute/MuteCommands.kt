package top.likoslupus.cellulosesz.administration.moderation.mute

import com.mojang.brigadier.arguments.StringArgumentType
import com.mojang.brigadier.context.CommandContext
import net.minecraft.commands.CommandSourceStack
import top.likoslupus.cellulosesz.administration.config.ModerationSettings
import top.likoslupus.cellulosesz.administration.format.AdministrationMessages
import top.likoslupus.cellulosesz.administration.moderation.command.*
import top.likoslupus.cellulosesz.administration.moderation.moderationActor
import top.likoslupus.cellulosesz.administration.moderation.notify.ModerationNotifier
import top.likoslupus.cellulosesz.core.command.CommandCategory
import top.likoslupus.cellulosesz.core.command.CommandFeedbackTarget
import top.likoslupus.cellulosesz.core.command.dsl.CommandDefinition
import top.likoslupus.cellulosesz.core.command.dsl.command
import top.likoslupus.cellulosesz.core.command.dsl.greedyString
import top.likoslupus.cellulosesz.core.command.dsl.word
import top.likoslupus.cellulosesz.core.permission.CommandPermissions
import top.likoslupus.cellulosesz.core.runtime.RuntimeKernel
import top.likoslupus.cellulosesz.core.text.MessageKey
import top.likoslupus.cellulosesz.core.text.adventure.AdventureRuntime
import top.likoslupus.cellulosesz.core.text.adventure.replyError
import top.likoslupus.cellulosesz.core.text.i18n.LocalizedMessages
import top.likoslupus.cellulosesz.core.text.model.LocalizedText
import top.likoslupus.cellulosesz.core.text.presentation.TextPresentation
import top.likoslupus.cellulosesz.core.text.presentation.replyDetail
import top.likoslupus.cellulosesz.core.text.presentation.sendNotices
import java.time.Instant

/** `/mute`, `/tempmute`, `/unmute` and `/muteinfo`. */
internal object MuteCommands {

    fun commands(
        mutes: MuteService,
        notifier: ModerationNotifier,
        settings: () -> ModerationSettings,
        kernel: RuntimeKernel,
        messages: LocalizedMessages,
        adventure: AdventureRuntime,
        presentation: TextPresentation,
    ): List<CommandDefinition> = listOf(
        command(
            name = "mute",
            category = CommandCategory.ADMINISTRATION,
            permission = CommandPermissions.MUTE,
            documentation = "administration/mute",
        ) {
            argument("player", word()) {
                executes {
                    mute(
                        context,
                        null,
                        false,
                        mutes,
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
                        mute(
                            context,
                            get(reason),
                            false,
                            mutes,
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
            name = "tempmute",
            category = CommandCategory.ADMINISTRATION,
            permission = CommandPermissions.TEMP_MUTE,
            documentation = "administration/tempmute",
        ) {
            argument("player", word()) {
                argument("duration", word()) {
                    executes {
                        mute(
                            context,
                            null,
                            true,
                            mutes,
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
                            mute(
                                context,
                                get(reason),
                                true,
                                mutes,
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
            }
        },

        command(
            name = "unmute",
            category = CommandCategory.ADMINISTRATION,
            permission = CommandPermissions.UNMUTE,
            documentation = "administration/unmute",
        ) {
            argument("player", word()) { player ->
                executes {
                    val source = context.source
                    val target = get(player)
                    val actor = source.moderationActor()
                    val playerId = source.player?.uuid
                    moderationLaunch(
                        source,
                        kernel,
                        messages,
                        adventure,
                        presentation,
                        LocalizedText(MessageKey("administration.pending.unmute")),
                    ) { feedback ->
                        val result = mutes.unmute(actor, target)
                        if (result is UnmuteResult.Unmuted) {
                            kernel.sendNotices(
                                CommandFeedbackTarget.Player(result.target.id),
                                adventure,
                                presentation,
                                result.target.id,
                                listOf(AdministrationMessages.unmutedTargetNotice()),
                            )
                        }
                        kernel.sendNotices(
                            feedback,
                            adventure,
                            presentation,
                            playerId,
                            listOf(AdministrationMessages.unmute(result)),
                        )
                    }
                }
            }
        },

        command(
            name = "muteinfo",
            category = CommandCategory.ADMINISTRATION,
            permission = CommandPermissions.MUTE_INFO,
            documentation = "administration/muteinfo",
        ) {
            argument("player", word()) { player ->
                executes {
                    val source = context.source
                    val playerId = source.player?.uuid
                    when (val mute = mutes.info(get(player))) {
                        null -> source.replyError(
                            adventure,
                            messages.renderFor(
                                playerId,
                                MessageKey("administration.muteinfo.not_muted")
                            ),
                        )

                        else -> source.replyDetail(
                            adventure,
                            presentation,
                            playerId,
                            AdministrationMessages.muteInfo(mute, Instant.now()),
                        )
                    }
                }
            }
        },
    )

    private fun mute(
        context: CommandContext<CommandSourceStack>,
        rawReason: String?,
        temporary: Boolean,
        mutes: MuteService,
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
            config.defaultMuteReason,
            config.maxReasonLength
        ) ?: return invalidReason(source, messages, adventure)
        val target = StringArgumentType.getString(context, "player")
        val duration = durationFor(context, temporary)
        if (temporary && duration == null) {
            return invalidDuration(source, messages, adventure)
        }
        val actor = source.moderationActor()
        val playerId = source.player?.uuid

        return moderationLaunch(
            source,
            kernel,
            messages,
            adventure,
            presentation,
            LocalizedText(MessageKey("administration.pending.mute")),
        ) { feedback ->
            val now = Instant.now()
            val result = mutes.mute(actor, target, reason, duration)
            if (result is MuteResult.Applied) {
                kernel.sendNotices(
                    CommandFeedbackTarget.Player(result.target.id),
                    adventure,
                    presentation,
                    result.target.id,
                    listOf(AdministrationMessages.mutedTargetNotice(reason.value)),
                )
                if (config.notifyModerators) {
                    notifyModerators(
                        kernel,
                        notifier,
                        AdministrationMessages.notifyMute(
                            actor.displayName,
                            result.target.name,
                            result.expiresAt,
                            now,
                        )
                    )
                }
            }
            kernel.sendNotices(
                feedback,
                adventure,
                presentation,
                playerId,
                listOf(AdministrationMessages.mute(result, now)),
            )
        }
    }

}
