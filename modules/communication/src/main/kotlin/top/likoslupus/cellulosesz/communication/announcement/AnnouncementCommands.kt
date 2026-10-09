package top.likoslupus.cellulosesz.communication.announcement

import com.mojang.brigadier.CommandDispatcher
import com.mojang.brigadier.arguments.StringArgumentType
import com.mojang.brigadier.context.CommandContext
import net.minecraft.commands.CommandSourceStack
import net.minecraft.commands.Commands
import net.minecraft.commands.arguments.DimensionArgument
import top.likoslupus.cellulosesz.communication.command.communicationLaunch
import top.likoslupus.cellulosesz.communication.format.CommunicationMessages
import top.likoslupus.cellulosesz.core.command.message
import top.likoslupus.cellulosesz.core.command.requiresPermission
import top.likoslupus.cellulosesz.core.permission.CommandPermissions
import top.likoslupus.cellulosesz.core.permission.PermissionService
import top.likoslupus.cellulosesz.core.runtime.RuntimeKernel

/** `/broadcast` and `/broadcastworld`: moderator-only literal announcements. */
internal object AnnouncementCommands {

    fun register(
        dispatcher: CommandDispatcher<CommandSourceStack>,
        announcements: AnnouncementService,
        kernel: RuntimeKernel,
        permissions: PermissionService,
    ) {
        dispatcher.register(
            Commands.literal("broadcast")
                    .requires { it.requiresPermission(permissions, CommandPermissions.BROADCAST) }
                    .then(
                        Commands.argument("message", StringArgumentType.greedyString())
                                .executes { context ->
                                    deliver(context, announcements, kernel, null)
                                }
                    )
        )
        dispatcher.register(
            Commands.literal("broadcastworld")
                    .requires {
                        it.requiresPermission(
                            permissions,
                            CommandPermissions.BROADCAST_WORLD
                        )
                    }
                    .then(
                        Commands.argument("dimension", DimensionArgument.dimension())
                                .then(
                                    Commands.argument(
                                        "message",
                                        StringArgumentType.greedyString()
                                    )
                                            .executes { context ->
                                                val dimension = DimensionArgument.getDimension(
                                                    context,
                                                    "dimension"
                                                ).dimension().identifier().toString()
                                                deliver(context, announcements, kernel, dimension)
                                            }
                                )
                    )
        )
    }

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
