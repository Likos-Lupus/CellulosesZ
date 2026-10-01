package top.likoslupus.cellulosesz.playerstate

import com.mojang.brigadier.CommandDispatcher
import com.mojang.brigadier.arguments.StringArgumentType
import com.mojang.brigadier.context.CommandContext
import net.minecraft.commands.CommandSourceStack
import net.minecraft.commands.Commands
import net.minecraft.server.level.ServerPlayer
import net.minecraft.server.permissions.Permissions
import net.minecraft.world.entity.player.Abilities
import top.likoslupus.cellulosesz.command.reply
import top.likoslupus.cellulosesz.command.replyError
import top.likoslupus.cellulosesz.player.PlayerResolver
import top.likoslupus.cellulosesz.text.Messages

internal object PlayerStateCommands {

    fun register(dispatcher: CommandDispatcher<CommandSourceStack>) {
        dispatcher.register(
            Commands.literal("fly")
                    .requires { it.permissions().hasPermission(Permissions.COMMANDS_MODERATOR) }
                    .executes { context -> fly(context, null) }
                    .then(
                        Commands.argument("player", StringArgumentType.word())
                                .executes { context ->
                                    fly(
                                        context,
                                        StringArgumentType.getString(context, "player")
                                    )
                                }
                    )
        )
        dispatcher.register(
            Commands.literal("god")
                    .requires { it.permissions().hasPermission(Permissions.COMMANDS_MODERATOR) }
                    .executes { context -> god(context, null) }
                    .then(
                        Commands.argument("player", StringArgumentType.word())
                                .executes { context ->
                                    god(
                                        context,
                                        StringArgumentType.getString(context, "player")
                                    )
                                }
                    )
        )
    }

    private fun fly(
        context: CommandContext<CommandSourceStack>,
        targetName: String?
    ): Int {
        val source = context.source
        val target = resolveTarget(source, targetName)
            ?: return source.replyError(Messages.prefixed("player not found"))
        val current = target.abilities.pack()
        val enable = !current.mayFly()
        target.abilities.apply(
            Abilities.Packed(
                current.invulnerable(),
                enable,
                enable,
                current.instabuild(),
                current.mayBuild(),
                current.flyingSpeed(),
                current.walkingSpeed(),
            )
        )
        target.onUpdateAbilities()
        val state = if (enable) "enabled" else "disabled"
        return source.reply(Messages.prefixed("flight $state for ${target.name.string}"))
    }

    private fun god(context: CommandContext<CommandSourceStack>, targetName: String?): Int {
        val source = context.source
        val target = resolveTarget(source, targetName)
            ?: return source.replyError(Messages.prefixed("player not found"))
        val enable = !target.isInvulnerable
        target.isInvulnerable = enable
        val state = if (enable) "enabled" else "disabled"
        return source.reply(Messages.prefixed("god mode $state for ${target.name.string}"))
    }

    private fun resolveTarget(
        source: CommandSourceStack,
        targetName: String?
    ): ServerPlayer? =
        if (targetName == null) {
            source.player
        } else {
            PlayerResolver.onlineByName(
                source.server,
                targetName
            )
        }

}
