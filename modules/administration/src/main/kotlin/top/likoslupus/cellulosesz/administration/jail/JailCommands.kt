package top.likoslupus.cellulosesz.administration.jail

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
import top.likoslupus.cellulosesz.core.text.presentation.replyPage
import top.likoslupus.cellulosesz.core.text.presentation.sendNotices
import top.likoslupus.cellulosesz.foundation.time.DurationParser
import java.time.Instant

/** `/setjail`, `/jail`, `/unjail`, `/togglejail`, `/deljail`, `/jails` and `/jailedplayers`. */
internal object JailCommands {

    private const val DEFAULT_JAIL_REASON: String = "Jailed by an administrator"
    private const val PERMANENT: String = "permanent"

    fun commands(
        jail: JailService,
        notifier: ModerationNotifier,
        moderationSettings: () -> ModerationSettings,
        kernel: RuntimeKernel,
        messages: LocalizedMessages,
        adventure: AdventureRuntime,
        presentation: TextPresentation,
    ): List<CommandDefinition> = listOf(
        command(
            name = "setjail",
            category = CommandCategory.ADMINISTRATION,
            permission = CommandPermissions.SET_JAIL,
            documentation = "administration/setjail",
        ) {
            argument("name", word()) { name ->
                executesPlayer {
                    setJail(
                        context,
                        get(name),
                        jail,
                        kernel,
                        messages,
                        adventure,
                        presentation,
                    )
                }
            }
        },

        command(
            name = "jail",
            category = CommandCategory.ADMINISTRATION,
            permission = CommandPermissions.JAIL,
            documentation = "administration/jail",
        ) {
            argument("player", word()) { player ->
                argument("jail", word()) { jailName ->
                    suggests { jail.locations().forEach { suggest(it.name.value) } }
                    executes {
                        applyJail(
                            context,
                            get(player),
                            get(jailName),
                            null,
                            null,
                            jail,
                            notifier,
                            moderationSettings,
                            kernel,
                            messages,
                            adventure,
                            presentation,
                        )
                    }
                    argument("duration", word()) { duration ->
                        executes {
                            applyJail(
                                context,
                                get(player),
                                get(jailName),
                                get(duration),
                                null,
                                jail,
                                notifier,
                                moderationSettings,
                                kernel,
                                messages,
                                adventure,
                                presentation,
                            )
                        }
                        argument("reason", greedyString()) { reason ->
                            executes {
                                applyJail(
                                    context,
                                    get(player),
                                    get(jailName),
                                    get(duration),
                                    get(reason),
                                    jail,
                                    notifier,
                                    moderationSettings,
                                    kernel,
                                    messages,
                                    adventure,
                                    presentation,
                                )
                            }
                        }
                    }
                }
            }
        },

        command(
            name = "unjail",
            category = CommandCategory.ADMINISTRATION,
            permission = CommandPermissions.UNJAIL,
            documentation = "administration/unjail",
        ) {
            argument("player", word()) { player ->
                executes {
                    applyUnjail(
                        context,
                        get(player),
                        jail,
                        notifier,
                        kernel,
                        messages,
                        adventure,
                        presentation,
                    )
                }
            }
        },

        command(
            name = "togglejail",
            category = CommandCategory.ADMINISTRATION,
            permission = CommandPermissions.TOGGLE_JAIL,
            documentation = "administration/togglejail",
        ) {
            argument("player", word()) { player ->
                executes {
                    applyToggleJail(
                        context,
                        get(player),
                        null,
                        jail,
                        notifier,
                        moderationSettings,
                        kernel,
                        messages,
                        adventure,
                        presentation,
                    )
                }
                argument("reason", greedyString()) { reason ->
                    executes {
                        applyToggleJail(
                            context,
                            get(player),
                            get(reason),
                            jail,
                            notifier,
                            moderationSettings,
                            kernel,
                            messages,
                            adventure,
                            presentation,
                        )
                    }
                }
            }
        },

        command(
            name = "deljail",
            category = CommandCategory.ADMINISTRATION,
            permission = CommandPermissions.DEL_JAIL,
            documentation = "administration/deljail",
        ) {
            argument("name", word()) { name ->
                executes {
                    deleteJail(
                        context,
                        get(name),
                        jail,
                        kernel,
                        messages,
                        adventure,
                        presentation,
                    )
                }
            }
        },

        command(
            name = "jails",
            category = CommandCategory.ADMINISTRATION,
            permission = CommandPermissions.JAILS,
            documentation = "administration/jails",
        ) {
            executes {
                val source = context.source
                source.replyPage(
                    adventure,
                    presentation,
                    source.player?.uuid,
                    AdministrationMessages.jails(jail.locations()),
                )
            }
        },

        command(
            name = "jailedplayers",
            category = CommandCategory.ADMINISTRATION,
            permission = CommandPermissions.JAILED_PLAYERS,
            documentation = "administration/jailedplayers",
        ) {
            executes {
                val source = context.source
                source.replyPage(
                    adventure,
                    presentation,
                    source.player?.uuid,
                    AdministrationMessages.jailedPlayers(
                        jail.sentences(),
                        Instant.now(),
                    ),
                )
            }
        },
    )

    private fun setJail(
        context: CommandContext<CommandSourceStack>,
        rawName: String,
        jail: JailService,
        kernel: RuntimeKernel,
        messages: LocalizedMessages,
        adventure: AdventureRuntime,
        presentation: TextPresentation,
    ): Int {
        val source = context.source
        val player = source.player
            ?: return source.replyError(
                adventure,
                messages.renderFor(
                    null,
                    MessageKey("core.error.player_required")
                ),
            )
        val playerId = player.uuid
        val destination = JailDestination(
            dimension = player.level().dimension().identifier().toString(),
            x = player.x,
            y = player.y,
            z = player.z,
            yaw = player.yRot,
            pitch = player.xRot,
        )
        return moderationLaunch(
            source,
            kernel,
            messages,
            adventure,
            presentation,
            LocalizedText(MessageKey("administration.pending.setjail")),
        ) { feedback ->
            kernel.sendNotices(
                feedback,
                adventure,
                presentation,
                playerId,
                listOf(
                    AdministrationMessages.setJail(
                        jail.setLocation(
                            rawName,
                            destination,
                            player.gameProfile.name,
                        )
                    )
                ),
            )
        }
    }

    private fun applyJail(
        context: CommandContext<CommandSourceStack>,
        target: String,
        rawJail: String,
        rawDuration: String?,
        rawReason: String?,
        jail: JailService,
        notifier: ModerationNotifier,
        moderationSettings: () -> ModerationSettings,
        kernel: RuntimeKernel,
        messages: LocalizedMessages,
        adventure: AdventureRuntime,
        presentation: TextPresentation,
    ): Int {
        val source = context.source
        val config = moderationSettings()
        val reason = resolveReason(
            rawReason,
            DEFAULT_JAIL_REASON,
            config.maxReasonLength
        ) ?: return invalidReason(source, messages, adventure)
        val duration = when {
            rawDuration == null || rawDuration.equals(PERMANENT, ignoreCase = true) -> null
            else -> DurationParser.parse(rawDuration)
                ?: return invalidDuration(source, messages, adventure)
        }
        val actor = source.moderationActor()
        val playerId = source.player?.uuid
        return moderationLaunch(
            source,
            kernel,
            messages,
            adventure,
            presentation,
            LocalizedText(MessageKey("administration.pending.jail")),
        ) { feedback ->
            val now = Instant.now()
            val result = jail.jail(actor, target, rawJail, duration, reason)
            if (result is JailActionOutcome.Jailed) {
                kernel.sendNotices(
                    CommandFeedbackTarget.Player(result.target.id),
                    adventure,
                    presentation,
                    result.target.id,
                    listOf(AdministrationMessages.jailedTargetNotice(reason.value)),
                )
                if (config.notifyModerators) {
                    val expiresAt = result.expiresAt
                    notifyModerators(
                        kernel,
                        notifier,
                        if (expiresAt != null) {
                            AdministrationMessages.notifyJailTemp(
                                actor.displayName,
                                result.target.name,
                                expiresAt,
                                now,
                            )
                        } else {
                            AdministrationMessages.notifyJail(
                                actor.displayName,
                                result.target.name
                            )
                        },
                    )
                }
            }
            kernel.sendNotices(
                feedback,
                adventure,
                presentation,
                playerId,
                listOf(AdministrationMessages.jail(result, now)),
            )
        }
    }

    private fun applyUnjail(
        context: CommandContext<CommandSourceStack>,
        target: String,
        jail: JailService,
        notifier: ModerationNotifier,
        kernel: RuntimeKernel,
        messages: LocalizedMessages,
        adventure: AdventureRuntime,
        presentation: TextPresentation,
    ): Int {
        val source = context.source
        val actor = source.moderationActor()
        val playerId = source.player?.uuid
        return moderationLaunch(
            source,
            kernel,
            messages,
            adventure,
            presentation,
            LocalizedText(MessageKey("administration.pending.unjail")),
        ) { feedback ->
            val now = Instant.now()
            val result = jail.release(actor, target)
            if (result is JailActionOutcome.ReleaseRequested) {
                kernel.sendNotices(
                    CommandFeedbackTarget.Player(result.target.id),
                    adventure,
                    presentation,
                    result.target.id,
                    listOf(AdministrationMessages.releasedTargetNotice()),
                )
                notifyModerators(
                    kernel,
                    notifier,
                    AdministrationMessages.notifyUnjail(
                        actor.displayName,
                        result.target.name
                    ),
                )
            }
            kernel.sendNotices(
                feedback,
                adventure,
                presentation,
                playerId,
                listOf(AdministrationMessages.jail(result, now)),
            )
        }
    }

    private fun applyToggleJail(
        context: CommandContext<CommandSourceStack>,
        target: String,
        rawReason: String?,
        jail: JailService,
        notifier: ModerationNotifier,
        moderationSettings: () -> ModerationSettings,
        kernel: RuntimeKernel,
        messages: LocalizedMessages,
        adventure: AdventureRuntime,
        presentation: TextPresentation,
    ): Int {
        val source = context.source
        val config = moderationSettings()
        val reason = resolveReason(
            rawReason,
            DEFAULT_JAIL_REASON,
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
            LocalizedText(MessageKey("administration.pending.togglejail")),
        ) { feedback ->
            val now = Instant.now()
            val result = jail.toggle(actor, target, reason)
            when (result) {
                is JailActionOutcome.Jailed -> {
                    kernel.sendNotices(
                        CommandFeedbackTarget.Player(result.target.id),
                        adventure,
                        presentation,
                        result.target.id,
                        listOf(AdministrationMessages.jailedTargetNotice(reason.value)),
                    )
                    if (config.notifyModerators) {
                        notifyModerators(
                            kernel,
                            notifier,
                            AdministrationMessages.notifyJail(
                                actor.displayName,
                                result.target.name
                            ),
                        )
                    }
                }

                is JailActionOutcome.ReleaseRequested -> {
                    kernel.sendNotices(
                        CommandFeedbackTarget.Player(result.target.id),
                        adventure,
                        presentation,
                        result.target.id,
                        listOf(AdministrationMessages.releasedTargetNotice()),
                    )
                    notifyModerators(
                        kernel,
                        notifier,
                        AdministrationMessages.notifyUnjail(
                            actor.displayName,
                            result.target.name
                        ),
                    )
                }

                else -> Unit
            }
            kernel.sendNotices(
                feedback,
                adventure,
                presentation,
                playerId,
                listOf(AdministrationMessages.jail(result, now)),
            )
        }
    }

    private fun deleteJail(
        context: CommandContext<CommandSourceStack>,
        rawName: String,
        jail: JailService,
        kernel: RuntimeKernel,
        messages: LocalizedMessages,
        adventure: AdventureRuntime,
        presentation: TextPresentation,
    ): Int {
        val source = context.source
        val playerId = source.player?.uuid
        return moderationLaunch(
            source,
            kernel,
            messages,
            adventure,
            presentation,
            LocalizedText(MessageKey("administration.pending.deljail")),
        ) { feedback ->
            kernel.sendNotices(
                feedback,
                adventure,
                presentation,
                playerId,
                listOf(
                    AdministrationMessages.deleteJail(
                        rawName,
                        jail.deleteLocation(rawName)
                    )
                ),
            )
        }
    }

}