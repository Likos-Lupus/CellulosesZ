package top.likoslupus.cellulosesz.administration.moderation.mute

import com.mojang.brigadier.CommandDispatcher
import com.mojang.brigadier.arguments.StringArgumentType
import com.mojang.brigadier.context.CommandContext
import net.minecraft.commands.CommandSourceStack
import net.minecraft.commands.Commands
import top.likoslupus.cellulosesz.administration.config.ModerationSettings
import top.likoslupus.cellulosesz.administration.moderation.command.*
import top.likoslupus.cellulosesz.administration.moderation.moderationActor
import top.likoslupus.cellulosesz.administration.moderation.notify.ModerationNotifier
import top.likoslupus.cellulosesz.core.command.messagePlayer
import top.likoslupus.cellulosesz.core.command.reply
import top.likoslupus.cellulosesz.core.command.replyError
import top.likoslupus.cellulosesz.core.command.requiresPermission
import top.likoslupus.cellulosesz.core.permission.CommandPermissions
import top.likoslupus.cellulosesz.core.permission.PermissionService
import top.likoslupus.cellulosesz.core.runtime.RuntimeKernel
import top.likoslupus.cellulosesz.core.text.Messages
import java.time.Instant

/** `/mute`, `/tempmute`, `/unmute` and `/muteinfo`. */
internal object MuteCommands {

    fun register(
        dispatcher: CommandDispatcher<CommandSourceStack>,
        mutes: MuteService,
        notifier: ModerationNotifier,
        settings: () -> ModerationSettings,
        kernel: RuntimeKernel,
        permissions: PermissionService,
    ) {
        dispatcher.register(
            Commands.literal("mute")
                    .requires { it.requiresPermission(permissions, CommandPermissions.MUTE) }
                    .then(
                        Commands.argument("player", StringArgumentType.word())
                                .executes { context ->
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
                                .then(
                                    Commands.argument("reason", StringArgumentType.greedyString())
                                            .executes { context ->
                                                mute(
                                                    context,
                                                    StringArgumentType.getString(context, "reason"),
                                                    false,
                                                    mutes,
                                                    notifier,
                                                    settings,
                                                    kernel,
                                                )
                                            }
                                )
                    )
        )
        dispatcher.register(
            Commands.literal("tempmute")
                    .requires { it.requiresPermission(permissions, CommandPermissions.TEMP_MUTE) }
                    .then(
                        Commands.argument("player", StringArgumentType.word())
                                .then(
                                    Commands.argument("duration", StringArgumentType.word())
                                            .executes { context ->
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
                                            .then(
                                                Commands.argument(
                                                    "reason",
                                                    StringArgumentType.greedyString()
                                                )
                                                        .executes { context ->
                                                            mute(
                                                                context,
                                                                StringArgumentType.getString(
                                                                    context,
                                                                    "reason"
                                                                ),
                                                                true,
                                                                mutes,
                                                                notifier,
                                                                settings,
                                                                kernel,
                                                            )
                                                        }
                                            )
                                )
                    )
        )
        dispatcher.register(
            Commands.literal("unmute")
                    .requires { it.requiresPermission(permissions, CommandPermissions.UNMUTE) }
                    .then(
                        Commands.argument("player", StringArgumentType.word())
                                .executes { context ->
                                    val source = context.source
                                    val target = StringArgumentType.getString(context, "player")
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
                    )
        )
        dispatcher.register(
            Commands.literal("muteinfo")
                    .requires { it.requiresPermission(permissions, CommandPermissions.MUTE_INFO) }
                    .then(
                        Commands.argument("player", StringArgumentType.word())
                                .executes { context ->
                                    val source = context.source
                                    val target = StringArgumentType.getString(context, "player")
                                    val mute = mutes.info(target)
                                    if (mute == null) {
                                        source.replyError(Messages.prefixed("that player is not muted"))
                                    } else {
                                        source.reply(
                                            ModerationFeedback.muteInfo(
                                                mute,
                                                Instant.now()
                                            )
                                        )
                                    }
                                }
                    )
        )
    }

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
                        "${actor.displayName} muted ${result.target.name} ${
                            ModerationFeedback.expiry(result.expiresAt, Instant.now())
                        }",
                    )
                }
            }
            ModerationFeedback.mute(result, Instant.now())
        }
    }

}
