package top.likoslupus.cellulosesz.player.command

import net.minecraft.commands.CommandSourceStack
import top.likoslupus.cellulosesz.core.command.CommandCategory
import top.likoslupus.cellulosesz.core.command.dsl.CommandDefinition
import top.likoslupus.cellulosesz.core.command.dsl.command
import top.likoslupus.cellulosesz.core.command.dsl.word
import top.likoslupus.cellulosesz.core.permission.CommandPermissions
import top.likoslupus.cellulosesz.core.text.adventure.AdventureRuntime
import top.likoslupus.cellulosesz.core.text.presentation.TextPresentation
import top.likoslupus.cellulosesz.core.text.presentation.replyNotice
import top.likoslupus.cellulosesz.player.format.*

/**
 * `/ping [player]`: reports connection latency. Viewing another player's latency needs the
 * moderator `PING_OTHERS` node, expressed on the argument so Brigadier enforces it.
 */
internal object PingCommands {

    fun commands(
        adventure: AdventureRuntime,
        presentation: TextPresentation,
    ): List<CommandDefinition> = listOf(
        command(
            name = "ping",
            category = CommandCategory.PLAYER,
            permission = CommandPermissions.PING,
            documentation = "player/ping",
        ) {
            executesPlayer {
                val latency = source.player?.connection?.latency() ?: 0
                source.replyNotice(
                    adventure,
                    presentation,
                    playerId,
                    PlayerMessages.ping(latency)
                )
            }
            argument(
                "player",
                word(),
                permission = CommandPermissions.PING_OTHERS,
            ) { key ->
                suggests { suggestAll(onlineNames(source)) }
                executes {
                    other(
                        source,
                        get(key),
                        adventure,
                        presentation,
                    )
                }
            }
        },
    )

    private fun other(
        source: CommandSourceStack,
        name: String,
        adventure: AdventureRuntime,
        presentation: TextPresentation,
    ): Int {
        val requesterId = source.player?.uuid
        val target = source.server.playerList.players
                .firstOrNull { it.gameProfile.name.equals(name, ignoreCase = true) }
        val notice = when (target) {
            null -> PlayerMessages.notFound(name)
            else -> PlayerMessages.pingOther(
                target.gameProfile.name,
                target.connection.latency(),
            )
        }
        return source.replyNotice(
            adventure,
            presentation,
            requesterId,
            notice,
        )
    }

}
