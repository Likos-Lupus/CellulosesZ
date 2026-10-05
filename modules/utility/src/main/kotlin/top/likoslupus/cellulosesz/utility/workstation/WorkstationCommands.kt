package top.likoslupus.cellulosesz.utility.workstation

import com.mojang.brigadier.CommandDispatcher
import net.minecraft.commands.CommandSourceStack
import net.minecraft.commands.Commands.literal
import top.likoslupus.cellulosesz.core.command.reply
import top.likoslupus.cellulosesz.core.command.replyError
import top.likoslupus.cellulosesz.utility.format.UtilityMessages

internal object WorkstationCommands {

    fun register(
        dispatcher: CommandDispatcher<CommandSourceStack>,
        service: WorkstationService,
    ) {
        register(dispatcher, "workbench", WorkstationType.CRAFTING, service)
        register(dispatcher, "anvil", WorkstationType.ANVIL, service)
        register(dispatcher, "grindstone", WorkstationType.GRINDSTONE, service)
        register(dispatcher, "stonecutter", WorkstationType.STONECUTTER, service)
        register(dispatcher, "loom", WorkstationType.LOOM, service)
        register(dispatcher, "cartographytable", WorkstationType.CARTOGRAPHY, service)
        register(dispatcher, "smithingtable", WorkstationType.SMITHING, service)
    }

    private fun register(
        dispatcher: CommandDispatcher<CommandSourceStack>,
        literal: String,
        type: WorkstationType,
        service: WorkstationService,
    ) {
        dispatcher.register(
            literal(literal)
                    .executes { context ->
                        val source = context.source
                        val playerId = source.player?.uuid
                            ?: return@executes source.replyError(UtilityMessages.requiresPlayer())
                        source.reply(UtilityMessages.workstation(service.open(playerId, type)))
                    }
        )
    }

}
