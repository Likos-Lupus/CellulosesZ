package top.likoslupus.cellulosesz.utility.inspection

import top.likoslupus.cellulosesz.core.command.CommandCategory
import top.likoslupus.cellulosesz.core.command.dsl.CommandDefinition
import top.likoslupus.cellulosesz.core.command.dsl.command
import top.likoslupus.cellulosesz.core.command.dsl.word
import top.likoslupus.cellulosesz.core.permission.CommandPermissions
import top.likoslupus.cellulosesz.core.player.KnownPlayerResolver
import top.likoslupus.cellulosesz.core.text.MessageKey
import top.likoslupus.cellulosesz.core.text.adventure.AdventureRuntime
import top.likoslupus.cellulosesz.core.text.adventure.replyError
import top.likoslupus.cellulosesz.core.text.i18n.LocalizedMessages
import top.likoslupus.cellulosesz.core.text.presentation.TextPresentation
import top.likoslupus.cellulosesz.core.text.presentation.replyNotice
import top.likoslupus.cellulosesz.utility.format.UtilityMessages

internal object InspectionCommands {

    fun commands(
        service: InspectionService,
        known: KnownPlayerResolver,
        messages: LocalizedMessages,
        adventure: AdventureRuntime,
        presentation: TextPresentation,
    ): List<CommandDefinition> = listOf(
        command(
            name = "enderchest",
            category = CommandCategory.UTILITY,
            permission = CommandPermissions.ENDER_CHEST,
            documentation = "utility/enderchest",
        ) {
            executesPlayer {
                source.replyNotice(
                    adventure,
                    presentation,
                    playerId,
                    UtilityMessages.inspection(service.enderChest(playerId))
                )
            }
        },

        command(
            name = "disposal",
            category = CommandCategory.UTILITY,
            permission = CommandPermissions.DISPOSAL,
            documentation = "utility/disposal",
        ) {
            executesPlayer {
                source.replyNotice(
                    adventure,
                    presentation,
                    playerId,
                    UtilityMessages.inspection(service.disposal(playerId))
                )
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
                            adventure,
                            messages.renderFor(
                                playerId,
                                MessageKey("utility.error.player_not_found")
                            ),
                        )
                    source.replyNotice(
                        adventure,
                        presentation,
                        playerId,
                        UtilityMessages.inspection(
                            service.inspect(
                                playerId, targetId
                            )
                        ),
                    )
                }
            }
        },
    )

}
