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
import top.likoslupus.cellulosesz.core.command.message
import top.likoslupus.cellulosesz.core.permission.CommandPermissions
import top.likoslupus.cellulosesz.core.runtime.RuntimeKernel

/** `/broadcast` and `/broadcastworld`: moderator-only literal announcements. */
internal object AnnouncementCommands {

    fun commands(
        announcements: AnnouncementService,
        kernel: RuntimeKernel,
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
                        null
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
        dimensionId: String?,
    ): Int {
        val source = context.source
        val senderName = source.player?.gameProfile?.name ?: "Console"
        val text = StringArgumentType.getString(context, "message")
        return communicationLaunch(
            source,
            kernel,
            "sending announcement..."
        ) { target ->
            val result = when (dimensionId) {
                null -> announcements.broadcast(senderName, text)
                else -> announcements.broadcastWorld(senderName, dimensionId, text)
            }
            kernel.message(
                target,
                CommunicationMessages.announcementFeedback(result)
            )
        }
    }

}
