package top.likoslupus.cellulosesz.administration.moderation.ban

import com.mojang.brigadier.arguments.StringArgumentType
import com.mojang.brigadier.context.CommandContext
import net.minecraft.commands.CommandSourceStack
import top.likoslupus.cellulosesz.administration.config.ModerationSettings
import top.likoslupus.cellulosesz.administration.format.AdministrationMessages
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
import java.time.Instant

/** `/ban`, `/tempban` and `/unban` for accounts. */
internal object BanCommands {

    fun commands(
        bans: BanService,
        notifier: ModerationNotifier,
        settings: () -> ModerationSettings,
        kernel: RuntimeKernel,
        messages: LocalizedMessages,
        adventure: AdventureRuntime,
        presentation: TextPresentation,
    ): List<CommandDefinition> = listOf(
        command(
            name = "ban",
            category = CommandCategory.ADMINISTRATION,
            permission = CommandPermissions.BAN,
            documentation = "administration/ban",
        ) {
            argument("player", word()) {
                executes {
                    ban(
                        context,
                        null,
                        false,
                        bans,
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
                        ban(
                            context,
                            get(reason),
                            false,
                            bans,
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
            name = "tempban",
            category = CommandCategory.ADMINISTRATION,
            permission = CommandPermissions.TEMP_BAN,
            documentation = "administration/tempban",
        ) {
            argument("player", word()) {
                argument("duration", word()) {
                    executes {
                        ban(
                            context,
                            null,
                            true,
                            bans,
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
                            ban(
                                context,
                                get(reason),
                                true,
                                bans,
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
            name = "unban",
            category = CommandCategory.ADMINISTRATION,
            permission = CommandPermissions.UNBAN,
            documentation = "administration/unban",
        ) {
            argument("player", word()) { player ->
                executes {
                    val target = get(player)
                    val source = context.source
                    val actor = source.moderationActor()
                    val playerId = source.player?.uuid
                    moderationLaunch(
                        source,
                        kernel,
                        messages,
                        adventure,
                        presentation,
                        LocalizedText(MessageKey("administration.pending.unban")),
                    ) { feedback ->
                        kernel.sendNotices(
                            feedback,
                            adventure,
                            presentation,
                            playerId,
                            listOf(
                                AdministrationMessages.unban(
                                    bans.unban(
                                        actor,
                                        target
                                    )
                                )
                            ),
                        )
                    }
                }
            }
        },
    )

    private fun ban(
        context: CommandContext<CommandSourceStack>,
        rawReason: String?,
        temporary: Boolean,
        bans: BanService,
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
            config.defaultBanReason,
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
            LocalizedText(MessageKey("administration.pending.ban")),
        ) { feedback ->
            val now = Instant.now()
            val result = bans.ban(actor, target, reason, duration)
            if (result is BanResult.Applied
                && config.notifyModerators
            ) {
                notifyModerators(
                    kernel,
                    notifier,
                    AdministrationMessages.notifyBan(
                        actor.displayName,
                        result.target.name,
                        result.expiresAt,
                        now,
                    )
                )
            }
            kernel.sendNotices(
                feedback,
                adventure,
                presentation,
                playerId,
                listOf(AdministrationMessages.ban(result, now)),
            )
        }
    }

}
