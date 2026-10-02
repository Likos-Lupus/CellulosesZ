package top.likoslupus.cellulosesz.administration.moderation.ban

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
import java.time.Instant

/** `/banip`, `/tempbanip` and `/unbanip`. Target is an IP literal or an online player. */
internal object IpBanCommands {

    fun register(
        dispatcher: CommandDispatcher<CommandSourceStack>,
        bans: BanService,
        notifier: ModerationNotifier,
        settings: () -> ModerationSettings,
        kernel: RuntimeKernel,
    ) {
        dispatcher.register(
            Commands.literal("banip")
                    .requires { it.canUseModeratorCommands() }
                    .then(
                        Commands.argument("target", StringArgumentType.word())
                                .executes { context ->
                                    ipBan(
                                        context,
                                        null,
                                        false,
                                        bans,
                                        notifier,
                                        settings,
                                        kernel
                                    )
                                }
                                .then(
                                    Commands.argument("reason", StringArgumentType.greedyString())
                                            .executes { context ->
                                                ipBan(
                                                    context,
                                                    StringArgumentType.getString(context, "reason"),
                                                    false,
                                                    bans,
                                                    notifier,
                                                    settings,
                                                    kernel,
                                                )
                                            }
                                )
                    )
        )
        dispatcher.register(
            Commands.literal("tempbanip")
                    .requires { it.canUseModeratorCommands() }
                    .then(
                        Commands.argument("target", StringArgumentType.word())
                                .then(
                                    Commands.argument("duration", StringArgumentType.word())
                                            .executes { context ->
                                                ipBan(
                                                    context,
                                                    null,
                                                    true,
                                                    bans,
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
                                                            ipBan(
                                                                context,
                                                                StringArgumentType.getString(
                                                                    context,
                                                                    "reason"
                                                                ),
                                                                true,
                                                                bans,
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
            Commands.literal("unbanip")
                    .requires { it.canUseModeratorCommands() }
                    .then(
                        Commands.argument("target", StringArgumentType.word())
                                .executes { context ->
                                    val source = context.source
                                    val target = StringArgumentType.getString(context, "target")
                                    val actor = source.moderationActor()
                                    moderationLaunch(source, kernel, "processing IP unban...") {
                                        ModerationFeedback.ipUnban(bans.unbanIp(actor, target))
                                    }
                                }
                    )
        )
    }

    private fun ipBan(
        context: CommandContext<CommandSourceStack>,
        rawReason: String?,
        temporary: Boolean,
        bans: BanService,
        notifier: ModerationNotifier,
        settings: () -> ModerationSettings,
        kernel: RuntimeKernel,
    ): Int {
        val source = context.source
        val config = settings()
        val reason = resolveReason(
            rawReason,
            config.defaultBanReason,
            config.maxReasonLength
        ) ?: return invalidReason(source)
        val target = StringArgumentType.getString(context, "target")
        val duration = durationFor(context, temporary)
        if (temporary && duration == null) {
            return invalidDuration(source)
        }
        val actor = source.moderationActor()

        return moderationLaunch(
            source,
            kernel,
            "processing IP ban..."
        ) {
            val result = bans.banIp(actor, target, reason, duration)
            if (result is IpBanResult.Applied
                && config.notifyModerators
            ) {
                notifyModerators(
                    kernel,
                    notifier,
                    "${actor.displayName} banned an IP address"
                )
            }
            ModerationFeedback.ipBan(result, Instant.now())
        }
    }

}
