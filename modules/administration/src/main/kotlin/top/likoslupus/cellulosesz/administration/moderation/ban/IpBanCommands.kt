package top.likoslupus.cellulosesz.administration.moderation.ban

import com.mojang.brigadier.arguments.StringArgumentType
import com.mojang.brigadier.context.CommandContext
import net.minecraft.commands.CommandSourceStack
import top.likoslupus.cellulosesz.administration.config.ModerationSettings
import top.likoslupus.cellulosesz.administration.format.AdministrationMessages
import top.likoslupus.cellulosesz.administration.format.ipBan
import top.likoslupus.cellulosesz.administration.format.ipUnban
import top.likoslupus.cellulosesz.administration.format.notifyIpBan
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

/** `/banip`, `/tempbanip` and `/unbanip`. Target is an IP literal or an online player. */
internal object IpBanCommands {

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
            name = "banip",
            category = CommandCategory.ADMINISTRATION,
            permission = CommandPermissions.BAN_IP,
            documentation = "administration/banip",
        ) {
            argument("target", word()) {
                executes {
                    ipBan(
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
                        ipBan(
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
            name = "tempbanip",
            category = CommandCategory.ADMINISTRATION,
            permission = CommandPermissions.TEMP_BAN_IP,
            documentation = "administration/tempbanip",
        ) {
            argument("target", word()) {
                argument("duration", word()) {
                    executes {
                        ipBan(
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
                            ipBan(
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
            name = "unbanip",
            category = CommandCategory.ADMINISTRATION,
            permission = CommandPermissions.UNBAN_IP,
            documentation = "administration/unbanip",
        ) {
            argument("target", word()) { target ->
                executes {
                    val source = context.source
                    val actor = source.moderationActor()
                    val playerId = source.player?.uuid
                    moderationLaunch(
                        source,
                        kernel,
                        messages,
                        adventure,
                        presentation,
                        LocalizedText(MessageKey("administration.pending.ip_unban")),
                    ) { feedback ->
                        kernel.sendNotices(
                            feedback,
                            adventure,
                            presentation,
                            playerId,
                            listOf(
                                AdministrationMessages.ipUnban(
                                    bans.unbanIp(
                                        actor,
                                        get(target)
                                    )
                                )
                            ),
                        )
                    }
                }
            }
        },
    )

    private fun ipBan(
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
        val target = StringArgumentType.getString(context, "target")
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
            LocalizedText(MessageKey("administration.pending.ip_ban")),
        ) { feedback ->
            val now = Instant.now()
            val result = bans.banIp(actor, target, reason, duration)
            if (result is IpBanResult.Applied
                && config.notifyModerators
            ) {
                notifyModerators(
                    kernel,
                    notifier,
                    AdministrationMessages.notifyIpBan(actor.displayName)
                )
            }
            kernel.sendNotices(
                feedback,
                adventure,
                presentation,
                playerId,
                listOf(AdministrationMessages.ipBan(result, now)),
            )
        }
    }

}
