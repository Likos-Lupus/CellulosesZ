package top.likoslupus.cellulosesz.communication.staff

import com.mojang.brigadier.arguments.StringArgumentType
import net.minecraft.commands.CommandSourceStack
import top.likoslupus.cellulosesz.communication.command.communicationLaunch
import top.likoslupus.cellulosesz.communication.format.CommunicationMessages
import top.likoslupus.cellulosesz.core.command.CommandCategory
import top.likoslupus.cellulosesz.core.command.dsl.CommandDefinition
import top.likoslupus.cellulosesz.core.command.dsl.command
import top.likoslupus.cellulosesz.core.command.dsl.greedyString
import top.likoslupus.cellulosesz.core.command.message
import top.likoslupus.cellulosesz.core.permission.CommandPermissions
import top.likoslupus.cellulosesz.core.runtime.RuntimeKernel

/** `/helpop`: player-or-console staff support channel, intentionally not gated by mute. */
internal object HelpOpCommands {

    fun commands(
        helpOp: HelpOpService,
        kernel: RuntimeKernel,
    ): List<CommandDefinition> = listOf(
        command(
            name = "helpop",
            category = CommandCategory.COMMUNICATION,
            permission = CommandPermissions.HELP_OP,
            documentation = "communication/helpop",
        ) {
            argument("message", greedyString()) {
                executes {
                    val source: CommandSourceStack = source
                    val player = source.player
                    val senderId = player?.uuid
                    val senderName = player?.gameProfile?.name
                        ?: "Console"
                    val text = StringArgumentType.getString(context, "message")
                    communicationLaunch(
                        source,
                        kernel,
                        "sending help request..."
                    ) { target ->
                        kernel.message(
                            target,
                            CommunicationMessages.helpOpFeedback(
                                helpOp.submit(senderId, senderName, text)
                            )
                        )
                    }
                }
            }
        },
    )

}
