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
import top.likoslupus.cellulosesz.core.command.message
import top.likoslupus.cellulosesz.core.command.reply
import top.likoslupus.cellulosesz.core.command.replyError
import top.likoslupus.cellulosesz.core.permission.CommandPermissions
import top.likoslupus.cellulosesz.core.player.KnownPlayerResolver
import top.likoslupus.cellulosesz.core.runtime.RuntimeKernel

/** `/msgtoggle` and `/ignore`. Durable, so mutations are asynchronous and write-before-publish. */
internal object MessagingPreferenceCommands {

    fun commands(
        preferences: MessagingPreferencesService,
        known: KnownPlayerResolver,
        kernel: RuntimeKernel,
    ): List<CommandDefinition> = listOf(
        command(
            name = "msgtoggle",
            category = CommandCategory.COMMUNICATION,
            permission = CommandPermissions.MSG_TOGGLE,
            documentation = "communication/msgtoggle",
        ) {
            executesPlayer { showStatus(context, preferences) }
            literal("on") {
                executesPlayer {
                    setReceive(
                        context,
                        preferences,
                        kernel,
                        true
                    )
                }
            }
            literal("off") {
                executesPlayer {
                    setReceive(
                        context,
                        preferences,
                        kernel,
                        false
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
            executesPlayer { listIgnored(context, preferences, known) }
            literal("add") {
                argument("player", word()) {
                    executesPlayer {
                        changeIgnore(
                            context,
                            preferences,
                            known,
                            kernel,
                            add = true
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
                            add = false
                        )
                    }
                }
            }
        },
    )

    private fun showStatus(
        context: CommandContext<CommandSourceStack>,
        preferences: MessagingPreferencesService,
    ): Int {
        val source = context.source
        val player = source.player
            ?: return source.replyError(CommunicationMessages.requiresPlayer())
        val current = preferences.current(player.uuid)
            ?: return source.replyError(CommunicationMessages.storageUnavailable())
        return source.reply(
            CommunicationMessages.receiveStatus(current.receivePrivateMessages)
        )
    }

    private fun setReceive(
        context: CommandContext<CommandSourceStack>,
        preferences: MessagingPreferencesService,
        kernel: RuntimeKernel,
        enabled: Boolean,
    ): Int {
        val source = context.source
        val player = source.player
            ?: return source.replyError(CommunicationMessages.requiresPlayer())
        val playerId = player.uuid
        val playerName = player.gameProfile.name
        return communicationLaunch(
            source,
            kernel,
            "updating..."
        ) {
            kernel.message(
                it,
                CommunicationMessages.preferenceFeedback(
                    preferences.setReceivePrivateMessages(
                        playerId,
                        playerName,
                        enabled
                    )
                )
            )
        }
    }

    private fun listIgnored(
        context: CommandContext<CommandSourceStack>,
        preferences: MessagingPreferencesService,
        known: KnownPlayerResolver,
    ): Int {
        val source = context.source
        val player = source.player
            ?: return source.replyError(CommunicationMessages.requiresPlayer())
        val current = preferences.current(player.uuid)
            ?: return source.replyError(CommunicationMessages.storageUnavailable())
        val names = current.ignoredPlayerIds
                .map { known.knownById(it)?.name ?: it.toString() }
                .sorted()
        return source.reply(CommunicationMessages.ignoreList(names))
    }

    private fun changeIgnore(
        context: CommandContext<CommandSourceStack>,
        preferences: MessagingPreferencesService,
        known: KnownPlayerResolver,
        kernel: RuntimeKernel,
        add: Boolean,
    ): Int {
        val source = context.source
        val player = source.player
            ?: return source.replyError(CommunicationMessages.requiresPlayer())
        val rawTarget = StringArgumentType.getString(context, "player")
        val target = known.onlineByName(rawTarget)
            ?: known.knownByName(rawTarget)
            ?: return source.replyError(
                CommunicationMessages.prefixed("player '$rawTarget' is not known to this server")
            )
        if (target.id == player.uuid) {
            return source.replyError(CommunicationMessages.prefixed("you cannot ignore yourself"))
        }

        val playerId = player.uuid
        val playerName = player.gameProfile.name
        val targetId = target.id
        val targetName = target.name
        return communicationLaunch(
            source,
            kernel,
            "updating..."
        ) { feedback ->
            val result = when (add) {
                true -> preferences.ignorePlayer(playerId, playerName, targetId)
                false -> preferences.unignorePlayer(playerId, playerName, targetId)
            }
            val component = when (result) {
                is IgnoreUpdateResult.Updated -> when (add) {
                    true -> CommunicationMessages.ignoreUpdated(targetName)
                    false -> CommunicationMessages.noLongerIgnoring(targetName)
                }

                else -> CommunicationMessages.ignoreFeedback(result)
            }
            kernel.message(feedback, component)
        }
    }

}
