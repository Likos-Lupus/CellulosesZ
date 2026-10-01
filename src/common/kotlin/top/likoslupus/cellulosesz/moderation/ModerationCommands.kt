package top.likoslupus.cellulosesz.moderation

import com.mojang.brigadier.CommandDispatcher
import com.mojang.brigadier.arguments.StringArgumentType
import com.mojang.brigadier.context.CommandContext
import net.minecraft.commands.CommandSourceStack
import net.minecraft.commands.Commands
import net.minecraft.network.chat.Component
import net.minecraft.server.level.ServerPlayer
import net.minecraft.server.permissions.Permissions
import top.likoslupus.cellulosesz.command.reply
import top.likoslupus.cellulosesz.command.replyError
import top.likoslupus.cellulosesz.player.PlayerResolver
import top.likoslupus.cellulosesz.text.Messages

internal object ModerationCommands {

    fun register(dispatcher: CommandDispatcher<CommandSourceStack>) {
        dispatcher.register(
            Commands.literal("heal")
                    .requires { it.permissions().hasPermission(Permissions.COMMANDS_MODERATOR) }
                    .executes { context -> heal(context, null) }
                    .then(
                        Commands.argument("player", StringArgumentType.word())
                                .executes { context ->
                                    heal(
                                        context,
                                        StringArgumentType.getString(context, "player")
                                    )
                                }
                    )
        )
        dispatcher.register(
            Commands.literal("feed")
                    .requires { it.permissions().hasPermission(Permissions.COMMANDS_MODERATOR) }
                    .executes { context -> feed(context, null) }
                    .then(
                        Commands.argument("player", StringArgumentType.word())
                                .executes { context ->
                                    feed(
                                        context,
                                        StringArgumentType.getString(context, "player")
                                    )
                                }
                    )
        )
        dispatcher.register(
            Commands.literal("kick")
                    .requires { it.permissions().hasPermission(Permissions.COMMANDS_MODERATOR) }
                    .then(
                        Commands.argument("player", StringArgumentType.word())
                                .executes { context ->
                                    kick(
                                        context,
                                        StringArgumentType.getString(context, "player"),
                                        null
                                    )
                                }
                                .then(
                                    Commands.argument("reason", StringArgumentType.greedyString())
                                            .executes { context ->
                                                kick(
                                                    context,
                                                    StringArgumentType.getString(context, "player"),
                                                    StringArgumentType.getString(context, "reason"),
                                                )
                                            }
                                )
                    )
        )
    }

    private fun heal(
        context: CommandContext<CommandSourceStack>,
        targetName: String?
    ): Int {
        val source = context.source
        val target = resolveTarget(source, targetName)
            ?: return source.replyError(Messages.prefixed("player not found"))
        target.health = target.maxHealth
        return source.reply(Messages.prefixed("healed ${target.name.string}"))
    }

    private fun feed(
        context: CommandContext<CommandSourceStack>,
        targetName: String?
    ): Int {
        val source = context.source
        val target = resolveTarget(source, targetName)
            ?: return source.replyError(Messages.prefixed("player not found"))
        target.foodData.foodLevel = 20
        target.foodData.setSaturation(20f)
        return source.reply(Messages.prefixed("fed ${target.name.string}"))
    }

    private fun kick(
        context: CommandContext<CommandSourceStack>,
        targetName: String,
        reason: String?
    ): Int {
        val source = context.source
        val target = PlayerResolver.onlineByName(source.server, targetName)
            ?: return source.replyError(Messages.prefixed("player '$targetName' is not online"))
        target.connection.disconnect(Component.literal(reason ?: "Kicked by an operator"))
        return source.reply(Messages.prefixed("kicked ${target.name.string}"))
    }

    private fun resolveTarget(
        source: CommandSourceStack,
        targetName: String?
    ): ServerPlayer? =
        if (targetName == null) {
            source.player
        } else {
            PlayerResolver.onlineByName(source.server, targetName)
        }

}
