package top.likoslupus.cellulosesz.communication.action.command

import net.minecraft.commands.CommandSourceStack
import top.likoslupus.cellulosesz.communication.action.ActionMessageResult
import top.likoslupus.cellulosesz.communication.action.ActionMessageService
import top.likoslupus.cellulosesz.communication.command.communicationLaunch
import top.likoslupus.cellulosesz.communication.format.CommunicationMessages
import top.likoslupus.cellulosesz.core.command.CommandCategory
import top.likoslupus.cellulosesz.core.command.dsl.CommandDefinition
import top.likoslupus.cellulosesz.core.command.dsl.command
import top.likoslupus.cellulosesz.core.command.dsl.message
import top.likoslupus.cellulosesz.core.permission.CommandPermissions
import top.likoslupus.cellulosesz.core.runtime.RuntimeKernel
import top.likoslupus.cellulosesz.core.text.MessageKey
import top.likoslupus.cellulosesz.core.text.adventure.AdventureRuntime
import top.likoslupus.cellulosesz.core.text.i18n.LocalizedMessages
import top.likoslupus.cellulosesz.core.text.presentation.TextPresentation
import top.likoslupus.cellulosesz.core.text.presentation.sendNotices

/**
 * `/me <action>`: the CellulosesZ entry point for action messages. When CellulosesZ takes over the
 * vanilla literal, the registry replaces the vanilla `/me` node with this one and keeps the original
 * reachable at `/minecraft:me`, so this definition must keep the vanilla `MessageArgument` node
 * shape.
 *
 * The protected `/czme` fallback is not declared here: the route planner derives it from the vanilla
 * conflict catalog, so it survives even when ordinary aliases are disabled. When the action service
 * is disabled the whole definition is omitted, so neither `/me` nor `/czme` is registered.
 */
internal object ActionMessageCommands {

    fun commands(
        actions: ActionMessageService,
        kernel: RuntimeKernel,
        messages: LocalizedMessages,
        adventure: AdventureRuntime,
        presentation: TextPresentation,
        enabled: Boolean,
    ): List<CommandDefinition> =
        when {
            !enabled -> emptyList()
            else -> listOf(
                command(
                    name = "me",
                    category = CommandCategory.COMMUNICATION,
                    permission = CommandPermissions.ME,
                    documentation = "communication/me",
                ) {
                    argument("action", message()) { key ->
                        executes {
                            emit(
                                source,
                                get(key).string,
                                actions,
                                kernel,
                                messages,
                                adventure,
                                presentation,
                            )
                        }
                    }
                },
            )
        }

    private fun emit(
        source: CommandSourceStack,
        rawText: String,
        actions: ActionMessageService,
        kernel: RuntimeKernel,
        messages: LocalizedMessages,
        adventure: AdventureRuntime,
        presentation: TextPresentation,
    ): Int {
        val player = source.player
        val senderId = player?.uuid
        val senderName = player?.gameProfile?.name ?: "Console"
        val dimensionId = player?.level()?.dimension()?.identifier()?.toString()
        return communicationLaunch(
            source,
            kernel,
            messages,
            adventure,
            MessageKey("communication.pending.action"),
        ) { target ->
            val result = actions.emit(senderId, senderName, dimensionId, rawText)
            if (result is ActionMessageResult.Delivered
                && senderId != null
            ) {
                return@communicationLaunch
            }
            kernel.sendNotices(
                target,
                adventure,
                presentation,
                senderId,
                listOf(CommunicationMessages.actionFeedback(result)),
            )
        }
    }

}
