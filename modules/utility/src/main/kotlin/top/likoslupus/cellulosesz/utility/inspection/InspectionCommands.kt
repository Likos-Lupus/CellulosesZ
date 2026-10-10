package top.likoslupus.cellulosesz.utility.inspection

import top.likoslupus.cellulosesz.core.command.CommandCategory
import top.likoslupus.cellulosesz.core.command.dsl.CommandDefinition
import top.likoslupus.cellulosesz.core.command.dsl.command
import top.likoslupus.cellulosesz.core.command.dsl.word
import top.likoslupus.cellulosesz.core.command.reply
import top.likoslupus.cellulosesz.core.command.replyError
import top.likoslupus.cellulosesz.core.permission.CommandPermissions
import top.likoslupus.cellulosesz.core.player.KnownPlayerResolver
import top.likoslupus.cellulosesz.utility.format.UtilityMessages

internal object InspectionCommands {

    fun commands(
        service: InspectionService,
        known: KnownPlayerResolver,
    ): List<CommandDefinition> = listOf(
        command(
            name = "enderchest",
            category = CommandCategory.UTILITY,
            permission = CommandPermissions.ENDER_CHEST,
            documentation = "utility/enderchest",
        ) {
            executesPlayer {
                source.reply(UtilityMessages.inspection(service.enderChest(playerId)))
            }
        },

        command(
            name = "disposal",
            category = CommandCategory.UTILITY,
            permission = CommandPermissions.DISPOSAL,
            documentation = "utility/disposal",
        ) {
            executesPlayer {
                source.reply(UtilityMessages.inspection(service.disposal(playerId)))
            }
        },

        command(
            name = "invsee",
            category = CommandCategory.UTILITY,
            permission = CommandPermissions.INVSEE,
            documentation = "utility/invsee",
        ) {
            argument("player", word()) { player ->
                suggests {
                    source.server.playerList.players.forEach {
                        suggest(it.gameProfile.name)
                    }
                }
                executesPlayer {
                    val raw = get(player)
                    val targetId = known.onlineByName(raw)?.id
                        ?: return@executesPlayer source.replyError(
                            UtilityMessages.prefixed("player not found or offline")
                        )
                    source.reply(
                        UtilityMessages.inspection(
                            service.inspect(
                                playerId,
                                targetId
                            )
                        )
                    )
                }
            }
        },
    )

}
