package top.likoslupus.cellulosesz.administration.playerstate

import com.mojang.brigadier.context.CommandContext
import net.minecraft.commands.CommandSourceStack
import net.minecraft.server.level.ServerPlayer
import net.minecraft.world.entity.player.Abilities
import top.likoslupus.cellulosesz.core.command.CommandCategory
import top.likoslupus.cellulosesz.core.command.dsl.CommandDefinition
import top.likoslupus.cellulosesz.core.command.dsl.command
import top.likoslupus.cellulosesz.core.command.dsl.word
import top.likoslupus.cellulosesz.core.command.reply
import top.likoslupus.cellulosesz.core.command.replyError
import top.likoslupus.cellulosesz.core.permission.CommandPermissions
import top.likoslupus.cellulosesz.core.player.PlayerResolver
import top.likoslupus.cellulosesz.core.text.Messages

/**
 * Operator control over individual player state (health, food, flight, invulnerability). This is not
 * moderation: nothing here is a sanction or is persisted.
 */
internal object PlayerStateCommands {

    fun commands(): List<CommandDefinition> = listOf(
        command(
            name = "heal",
            category = CommandCategory.ADMINISTRATION,
            permission = CommandPermissions.HEAL,
            documentation = "administration/heal",
        ) {
            executes { heal(context, null) }
            argument("player", word()) { player ->
                executes { heal(context, get(player)) }
            }
        },

        command(
            name = "feed",
            category = CommandCategory.ADMINISTRATION,
            permission = CommandPermissions.FEED,
            documentation = "administration/feed",
        ) {
            executes { feed(context, null) }
            argument("player", word()) { player ->
                executes { feed(context, get(player)) }
            }
        },

        command(
            name = "fly",
            category = CommandCategory.ADMINISTRATION,
            permission = CommandPermissions.FLY,
            documentation = "administration/fly",
        ) {
            executes { fly(context, null) }
            argument("player", word()) { player ->
                executes { fly(context, get(player)) }
            }
        },

        command(
            name = "god",
            category = CommandCategory.ADMINISTRATION,
            permission = CommandPermissions.GOD,
            documentation = "administration/god",
        ) {
            executes { god(context, null) }
            argument("player", word()) { player ->
                executes { god(context, get(player)) }
            }
        },
    )

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

    private fun god(
        context: CommandContext<CommandSourceStack>,
        targetName: String?
    ): Int {
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
