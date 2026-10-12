package top.likoslupus.cellulosesz.communication.announcement

import com.mojang.brigadier.arguments.StringArgumentType
import com.mojang.brigadier.context.CommandContext
import net.minecraft.commands.CommandSourceStack
import top.likoslupus.cellulosesz.communication.command.communicationLaunch
import top.likoslupus.cellulosesz.communication.format.CommunicationMessages
import top.likoslupus.cellulosesz.core.command.CommandCategory
import top.likoslupus.cellulosesz.core.command.dsl.CommandDefinition
import top.likoslupus.cellulosesz.core.command.dsl.command
import top.likoslupus.cellulosesz.core.command.dsl.dimension
import top.likoslupus.cellulosesz.core.command.dsl.greedyString
import top.likoslupus.cellulosesz.core.permission.CommandPermissions
import top.likoslupus.cellulosesz.core.runtime.RuntimeKernel
import top.likoslupus.cellulosesz.core.text.MessageKey
import top.likoslupus.cellulosesz.core.text.adventure.AdventureRuntime
import top.likoslupus.cellulosesz.core.text.i18n.LocalizedMessages
import top.likoslupus.cellulosesz.core.text.presentation.TextPresentation
import top.likoslupus.cellulosesz.core.text.presentation.sendNotices

/** `/broadcast` and `/broadcastworld`: moderator-only literal announcements. */
internal object AnnouncementCommands {

    fun commands(
        announcements: AnnouncementService,
        kernel: RuntimeKernel,
        messages: LocalizedMessages,
        adventure: AdventureRuntime,
        presentation: TextPresentation,
    ): List<CommandDefinition> = listOf(
        command(
            name = "broadcast",
            category = CommandCategory.COMMUNICATION,
            permission = CommandPermissions.BROADCAST,
            documentation = "communication/broadcast",
        ) {
            argument("message", greedyString()) {
                executes {
                    deliver(
                        context,
                        announcements,
                        kernel,
                        messages,
                        adventure,
                        presentation,
                        null,
                    )
                }
            }
        },

        command(
            name = "broadcastworld",
            category = CommandCategory.COMMUNICATION,
            permission = CommandPermissions.BROADCAST_WORLD,
            documentation = "communication/broadcastworld",
        ) {
            argument("dimension", dimension()) { dimension ->
                argument("message", greedyString()) {
                    executes {
                        deliver(
                            context,
                            announcements,
                            kernel,
                            messages,
                            adventure,
                            presentation,
                            get(dimension).toString(),
                        )
                    }
                }
            }
        },
    )

    private fun deliver(
        context: CommandContext<CommandSourceStack>,
        announcements: AnnouncementService,
        kernel: RuntimeKernel,
        messages: LocalizedMessages,
        adventure: AdventureRuntime,
        presentation: TextPresentation,
        dimensionId: String?,
    ): Int {
        val source = context.source
        val senderId = source.player?.uuid
        val senderName = source.player?.gameProfile?.name ?: "Console"
        val text = StringArgumentType.getString(context, "message")
        return communicationLaunch(
            source,
            kernel,
            messages,
            adventure,
            MessageKey("communication.pending.broadcast"),
        ) { target ->
            val result = when (dimensionId) {
                null -> announcements.broadcast(senderName, text)
                else -> announcements.broadcastWorld(senderName, dimensionId, text)
            }
            kernel.sendNotices(
                target,
                adventure,
                presentation,
                senderId,
                listOf(CommunicationMessages.announcementFeedback(result)),
            )
        }
    }

}
