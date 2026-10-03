package top.likoslupus.cellulosesz.communication.staff

import com.mojang.brigadier.CommandDispatcher
import com.mojang.brigadier.arguments.StringArgumentType
import net.minecraft.commands.CommandSourceStack
import net.minecraft.commands.Commands
import top.likoslupus.cellulosesz.communication.command.communicationLaunch
import top.likoslupus.cellulosesz.communication.format.CommunicationMessages
import top.likoslupus.cellulosesz.core.command.message
import top.likoslupus.cellulosesz.core.runtime.RuntimeKernel

/** `/helpop`: player-or-console staff support channel, intentionally not gated by mute. */
internal object HelpOpCommands {

    fun register(
        dispatcher: CommandDispatcher<CommandSourceStack>,
        helpOp: HelpOpService,
        kernel: RuntimeKernel,
    ) {
        dispatcher.register(
            Commands.literal("helpop")
                    .then(
                        Commands.argument("message", StringArgumentType.greedyString())
                                .executes { context ->
                                    val source = context.source
                                    val player = source.player
                                    val senderId = player?.uuid
                                    val senderName = player?.gameProfile?.name ?: "Console"
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
                    )
        )
    }

}
