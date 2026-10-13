package top.likoslupus.cellulosesz.utility.position

import top.likoslupus.cellulosesz.core.command.CommandCategory
import top.likoslupus.cellulosesz.core.command.dsl.CommandDefinition
import top.likoslupus.cellulosesz.core.command.dsl.command
import top.likoslupus.cellulosesz.core.permission.CommandPermissions
import top.likoslupus.cellulosesz.core.text.adventure.AdventureRuntime
import top.likoslupus.cellulosesz.core.text.presentation.TextPresentation
import top.likoslupus.cellulosesz.core.text.presentation.replyDetail
import top.likoslupus.cellulosesz.utility.format.UtilityMessages
import kotlin.math.floor

/** `/getpos` and `/depth`: read-only views of the sender's own position and depth. */
internal object PositionCommands {

    fun commands(
        adventure: AdventureRuntime,
        presentation: TextPresentation,
    ): List<CommandDefinition> = listOf(
        command(
            name = "getpos",
            category = CommandCategory.UTILITY,
            permission = CommandPermissions.GET_POS,
            documentation = "utility/getpos",
        ) {
            executesPlayer {
                val player = source.player ?: return@executesPlayer 0
                val level = player.level()
                source.replyDetail(
                    adventure,
                    presentation,
                    playerId,
                    UtilityMessages.position(
                        dimension = level.dimension().identifier().toString(),
                        x = floor(player.x).toInt(),
                        y = floor(player.y).toInt(),
                        z = floor(player.z).toInt(),
                    ),
                )
            }
        },

        command(
            name = "depth",
            category = CommandCategory.UTILITY,
            permission = CommandPermissions.DEPTH,
            documentation = "utility/depth",
        ) {
            executesPlayer {
                val player = source.player
                    ?: return@executesPlayer 0
                val level = player.level()
                source.replyDetail(
                    adventure,
                    presentation,
                    playerId,
                    UtilityMessages.depth(
                        DepthReading(
                            feetY = floor(player.y).toInt(),
                            minY = level.minY,
                            maxY = level.maxY,
                        )
                    ),
                )
            }
        },
    )

}
