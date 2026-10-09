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
import top.likoslupus.cellulosesz.core.command.requiresPermission
import top.likoslupus.cellulosesz.core.permission.CommandPermissions
import top.likoslupus.cellulosesz.core.permission.PermissionService
import top.likoslupus.cellulosesz.core.runtime.RuntimeKernel
import java.time.Instant

/** `/ban`, `/tempban` and `/unban` for accounts. */
internal object BanCommands {

    fun register(
        dispatcher: CommandDispatcher<CommandSourceStack>,
        bans: BanService,
        notifier: ModerationNotifier,
        settings: () -> ModerationSettings,
        kernel: RuntimeKernel,
        permissions: PermissionService,
    ) {
        dispatcher.register(
            Commands.literal("ban")
                    .requires { it.requiresPermission(permissions, CommandPermissions.BAN) }
                    .then(
                        Commands.argument("player", StringArgumentType.word())
                                .executes { context ->
                                    ban(
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
                                                ban(
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
            Commands.literal("tempban")
                    .requires { it.requiresPermission(permissions, CommandPermissions.TEMP_BAN) }
                    .then(
                        Commands.argument("player", StringArgumentType.word())
                                .then(
                                    Commands.argument("duration", StringArgumentType.word())
                                            .executes { context ->
                                                ban(
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
                                                            ban(
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
            Commands.literal("unban")
                    .requires { it.requiresPermission(permissions, CommandPermissions.UNBAN) }
                    .then(
                        Commands.argument("player", StringArgumentType.word())
                                .executes { context ->
                                    val source = context.source
                                    val target = StringArgumentType.getString(context, "player")
                                    val actor = source.moderationActor()
                                    moderationLaunch(source, kernel, "processing unban...") {
                                        ModerationFeedback.unban(bans.unban(actor, target))
                                    }
                                }
                    )
        )
    }

    private fun ban(
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
        val target = StringArgumentType.getString(context, "player")
        val duration = durationFor(context, temporary)
        if (temporary && duration == null) {
            return invalidDuration(source)
        }
        val actor = source.moderationActor()

        return moderationLaunch(
            source,
            kernel,
            "processing ban..."
        ) {
            val result = bans.ban(actor, target, reason, duration)
            if (result is BanResult.Applied
                && config.notifyModerators
            ) {
                notifyModerators(
                    kernel,
                    notifier,
                    "${actor.displayName} banned ${result.target.name} ${
                        ModerationFeedback.expiry(result.expiresAt, Instant.now())
                    }",
                )
            }
            ModerationFeedback.ban(result, Instant.now())
        }
    }

}
