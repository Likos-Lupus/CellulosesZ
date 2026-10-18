package top.likoslupus.cellulosesz.movement.compass

import top.likoslupus.cellulosesz.core.command.CommandCategory
import top.likoslupus.cellulosesz.core.command.dsl.CommandDefinition
import top.likoslupus.cellulosesz.core.command.dsl.command
import top.likoslupus.cellulosesz.core.permission.CommandPermissions
import top.likoslupus.cellulosesz.core.text.adventure.AdventureRuntime
import top.likoslupus.cellulosesz.core.text.presentation.TextPresentation
import top.likoslupus.cellulosesz.core.text.presentation.replyDetail
import top.likoslupus.cellulosesz.movement.format.MovementMessages

/** `/compass`: a read-only bearing reading. It mutates nothing and holds no state. */
internal object CompassCommands {

    fun commands(
        adventure: AdventureRuntime,
        presentation: TextPresentation,
    ): List<CommandDefinition> = listOf(
        command(
            name = "compass",
            category = CommandCategory.MOVEMENT,
            permission = CommandPermissions.COMPASS,
            documentation = "movement/compass",
        ) {
            executesPlayer {
                val player = source.player ?: return@executesPlayer 0
                return@executesPlayer source.replyDetail(
                    adventure,
                    presentation,
                    playerId,
                    MovementMessages.compass(CompassBearing.read(player.yRot)),
                )
            }
        },
    )

}
