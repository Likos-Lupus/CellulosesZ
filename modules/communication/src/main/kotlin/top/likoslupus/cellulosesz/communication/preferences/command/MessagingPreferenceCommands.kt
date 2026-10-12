package top.likoslupus.cellulosesz.communication.preferences.command

import com.mojang.brigadier.arguments.StringArgumentType
import com.mojang.brigadier.context.CommandContext
import net.minecraft.commands.CommandSourceStack
import top.likoslupus.cellulosesz.communication.command.communicationLaunch
import top.likoslupus.cellulosesz.communication.format.CommunicationMessages
import top.likoslupus.cellulosesz.communication.preferences.IgnoreUpdateResult
import top.likoslupus.cellulosesz.communication.preferences.MessagingPreferencesService
import top.likoslupus.cellulosesz.core.command.CommandCategory
import top.likoslupus.cellulosesz.core.command.dsl.CommandDefinition
import top.likoslupus.cellulosesz.core.command.dsl.command
import top.likoslupus.cellulosesz.core.command.dsl.word
import top.likoslupus.cellulosesz.core.permission.CommandPermissions
import top.likoslupus.cellulosesz.core.player.KnownPlayerResolver
import top.likoslupus.cellulosesz.core.runtime.RuntimeKernel
import top.likoslupus.cellulosesz.core.text.MessageKey
import top.likoslupus.cellulosesz.core.text.adventure.AdventureRuntime
import top.likoslupus.cellulosesz.core.text.adventure.replyError
import top.likoslupus.cellulosesz.core.text.i18n.LocalizedMessages
import top.likoslupus.cellulosesz.core.text.i18n.renderFor
import top.likoslupus.cellulosesz.core.text.presentation.TextPresentation
import top.likoslupus.cellulosesz.core.text.presentation.replyNotice
import top.likoslupus.cellulosesz.core.text.presentation.sendNotices

/** `/msgtoggle` and `/ignore`. Durable, so mutations are asynchronous and write-before-publish. */
internal object MessagingPreferenceCommands {

    fun commands(
        preferences: MessagingPreferencesService,
        known: KnownPlayerResolver,
        kernel: RuntimeKernel,
        messages: LocalizedMessages,
        adventure: AdventureRuntime,
        presentation: TextPresentation,
    ): List<CommandDefinition> = listOf(
        command(
            name = "msgtoggle",
            category = CommandCategory.COMMUNICATION,
            permission = CommandPermissions.MSG_TOGGLE,
            documentation = "communication/msgtoggle",
        ) {
            executesPlayer {
                showStatus(
                    context,
                    preferences,
                    messages,
                    adventure,
                    presentation
                )
            }
            literal("on") {
                executesPlayer {
                    setReceive(
                        context,
                        preferences,
                        kernel,
                        messages,
                        adventure,
                        presentation,
                        true,
                    )
                }
            }
            literal("off") {
                executesPlayer {
                    setReceive(
                        context,
                        preferences,
                        kernel,
                        messages,
                        adventure,
                        presentation,
                        false,
                    )
                }
            }
        },
        command(
            name = "ignore",
            category = CommandCategory.COMMUNICATION,
            permission = CommandPermissions.IGNORE,
            documentation = "communication/ignore",
        ) {
            executesPlayer {
                listIgnored(
                    context,
                    preferences,
                    known,
                    messages,
                    adventure,
                    presentation
                )
            }
            literal("add") {
                argument("player", word()) {
                    executesPlayer {
                        changeIgnore(
                            context,
                            preferences,
                            known,
                            kernel,
                            messages,
                            adventure,
                            presentation,
                            add = true,
                        )
                    }
                }
            }
            literal("remove") {
                argument("player", word()) {
                    executesPlayer {
                        changeIgnore(
                            context,
                            preferences,
                            known,
                            kernel,
                            messages,
                            adventure,
                            presentation,
                            add = false,
                        )
                    }
                }
            }
        },
    )

    private fun showStatus(
        context: CommandContext<CommandSourceStack>,
        preferences: MessagingPreferencesService,
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
                )
            )
        val current = preferences.current(player.uuid)
            ?: return source.replyError(
                adventure,
                messages.renderFor(
                    player.uuid,
                    MessageKey("communication.preference.storage_unavailable")
                ),
            )
        return source.replyNotice(
            adventure,
            presentation,
            player.uuid,
            CommunicationMessages.receiveStatus(current.receivePrivateMessages),
        )
    }

    private fun setReceive(
        context: CommandContext<CommandSourceStack>,
        preferences: MessagingPreferencesService,
        kernel: RuntimeKernel,
        messages: LocalizedMessages,
        adventure: AdventureRuntime,
        presentation: TextPresentation,
        enabled: Boolean,
    ): Int {
        val source = context.source
        val player = source.player
            ?: return source.replyError(
                adventure,
                messages.renderFor(
                    null,
                    MessageKey("core.error.player_required")
                )
            )
        val playerId = player.uuid
        val playerName = player.gameProfile.name
        return communicationLaunch(
            source,
            kernel,
            messages,
            adventure,
            MessageKey("communication.pending.updating"),
        ) { target ->
            kernel.sendNotices(
                target,
                adventure,
                presentation,
                playerId,
                listOf(
                    CommunicationMessages.preferenceFeedback(
                        preferences.setReceivePrivateMessages(
                            playerId,
                            playerName,
                            enabled,
                        )
                    )
                ),
            )
        }
    }

    private fun listIgnored(
        context: CommandContext<CommandSourceStack>,
        preferences: MessagingPreferencesService,
        known: KnownPlayerResolver,
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
                )
            )
        val current = preferences.current(player.uuid)
            ?: return source.replyError(
                adventure,
                messages.renderFor(
                    player.uuid,
                    MessageKey("communication.preference.storage_unavailable")
                ),
            )
        val names = current.ignoredPlayerIds
                .map { known.knownById(it)?.name ?: it.toString() }
                .sorted()
        return source.replyNotice(
            adventure,
            presentation,
            player.uuid,
            CommunicationMessages.ignoreList(names),
        )
    }

    private fun changeIgnore(
        context: CommandContext<CommandSourceStack>,
        preferences: MessagingPreferencesService,
        known: KnownPlayerResolver,
        kernel: RuntimeKernel,
        messages: LocalizedMessages,
        adventure: AdventureRuntime,
        presentation: TextPresentation,
        add: Boolean,
    ): Int {
        val source = context.source
        val player = source.player
            ?: return source.replyError(
                adventure,
                messages.renderFor(
                    null,
                    MessageKey("core.error.player_required")
                )
            )
        val rawTarget = StringArgumentType.getString(context, "player")
        val target = known.onlineByName(rawTarget)
            ?: known.knownByName(rawTarget)
            ?: return source.replyError(
                adventure,
                messages.renderFor(
                    player.uuid,
                    MessageKey("communication.preference.unknown_player"),
                    rawTarget
                ),
            )
        if (target.id == player.uuid) {
            return source.replyError(
                adventure,
                messages.renderFor(
                    player.uuid,
                    MessageKey("communication.preference.self_ignore")
                ),
            )
        }

        val playerId = player.uuid
        val playerName = player.gameProfile.name
        val targetId = target.id
        val targetName = target.name
        return communicationLaunch(
            source,
            kernel,
            messages,
            adventure,
            MessageKey("communication.pending.updating"),
        ) { feedback ->
            val result = when (add) {
                true -> preferences.ignorePlayer(playerId, playerName, targetId)
                false -> preferences.unignorePlayer(playerId, playerName, targetId)
            }
            val notice = when (result) {
                is IgnoreUpdateResult.Updated -> when (add) {
                    true -> CommunicationMessages.ignoreUpdated(targetName)
                    false -> CommunicationMessages.noLongerIgnoring(targetName)
                }

                else -> CommunicationMessages.ignoreFeedback(result, targetName)
            }
            kernel.sendNotices(
                feedback,
                adventure,
                presentation,
                playerId,
                listOf(notice)
            )
        }
    }

}
