package top.likoslupus.cellulosesz.administration.moderation.mute

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
import top.likoslupus.cellulosesz.core.command.messagePlayer
import top.likoslupus.cellulosesz.core.command.reply
import top.likoslupus.cellulosesz.core.command.replyError
import top.likoslupus.cellulosesz.core.permission.CommandPermissions
import top.likoslupus.cellulosesz.core.runtime.RuntimeKernel
import top.likoslupus.cellulosesz.core.text.Messages
import java.time.Instant

/** `/mute`, `/tempmute`, `/unmute` and `/muteinfo`. */
internal object MuteCommands {

    fun commands(
        mutes: MuteService,
        notifier: ModerationNotifier,
        settings: () -> ModerationSettings,
        kernel: RuntimeKernel,
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
                        kernel
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
                            kernel
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
                            kernel
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
                                kernel
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
                    moderationLaunch(source, kernel, "processing unmute...") {
                        val result = mutes.unmute(actor, target)
                        if (result is UnmuteResult.Unmuted) {
                            kernel.messagePlayer(
                                result.target.id,
                                Messages.prefixed("you have been unmuted"),
                            )
                        }
                        ModerationFeedback.unmute(result)
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
                    when (val mute = mutes.info(get(player))) {
                        null -> source.replyError(Messages.prefixed("that player is not muted"))
                        else -> source.reply(ModerationFeedback.muteInfo(mute, Instant.now()))
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
    ): Int {
        val source = context.source
        val config = settings()
        val reason = resolveReason(
            rawReason,
            config.defaultMuteReason,
            config.maxReasonLength
        ) ?: return invalidReason(source)
        val target = StringArgumentType.getString(context, "player")
        val duration = durationFor(context, temporary)
        if (temporary && duration == null) {
            return invalidDuration(source)
        }
        val actor = source.moderationActor()

        return moderationLaunch(
            source,
            kernel,
            "processing mute..."
        ) {
            val result = mutes.mute(actor, target, reason, duration)
            if (result is MuteResult.Applied) {
                kernel.messagePlayer(
                    result.target.id,
                    Messages.prefixed("you have been muted: ${reason.value}"),
                )
                if (config.notifyModerators) {
                    notifyModerators(
                        kernel,
                        notifier,
                        "${actor.displayName} muted ${result.target.name} " +
                                ModerationFeedback.expiry(result.expiresAt, Instant.now()),
                    )
                }
            }
            ModerationFeedback.mute(result, Instant.now())
        }
    }

}
