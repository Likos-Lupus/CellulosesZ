package top.likoslupus.cellulosesz.item

import com.mojang.brigadier.CommandDispatcher
import com.mojang.brigadier.arguments.StringArgumentType
import com.mojang.brigadier.context.CommandContext
import net.minecraft.commands.CommandSourceStack
import net.minecraft.commands.Commands
import net.minecraft.server.level.ServerPlayer
import net.minecraft.server.permissions.Permissions
import top.likoslupus.cellulosesz.command.reply
import top.likoslupus.cellulosesz.command.replyError
import top.likoslupus.cellulosesz.player.PlayerResolver
import top.likoslupus.cellulosesz.text.Messages

internal object ItemCommands {

    fun register(dispatcher: CommandDispatcher<CommandSourceStack>) {
        dispatcher.register(
            Commands.literal("repair")
                    .requires { it.permissions().hasPermission(Permissions.COMMANDS_MODERATOR) }
                    .executes { context -> repair(context, null) }
                    .then(
                        Commands.argument("player", StringArgumentType.word())
                                .executes { context ->
                                    repair(
                                        context,
                                        StringArgumentType.getString(context, "player")
                                    )
                                }
                    )
        )
    }

    private fun repair(context: CommandContext<CommandSourceStack>, targetName: String?): Int {
        val source = context.source
        val target = resolveTarget(source, targetName)
            ?: return source.replyError(Messages.prefixed("player not found"))

        val item = target.mainHandItem
        if (!item.isDamageableItem || item.damageValue == 0) {
            return source.replyError(Messages.prefixed("held item does not need repair"))
        }

        item.damageValue = 0
        return source.reply(Messages.prefixed("repaired ${target.name.string}'s held item"))
    }

    private fun resolveTarget(source: CommandSourceStack, targetName: String?): ServerPlayer? =
        if (targetName == null) source.player else PlayerResolver.onlineByName(
            source.server,
            targetName
        )

}
