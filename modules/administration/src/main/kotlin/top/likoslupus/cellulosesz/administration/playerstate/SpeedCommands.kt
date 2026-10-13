package top.likoslupus.cellulosesz.administration.playerstate

import top.likoslupus.cellulosesz.administration.format.AdministrationMessages
import top.likoslupus.cellulosesz.core.command.CommandCategory
import top.likoslupus.cellulosesz.core.command.dsl.*
import top.likoslupus.cellulosesz.core.permission.CommandPermissions
import top.likoslupus.cellulosesz.core.player.KnownPlayerResolver
import top.likoslupus.cellulosesz.core.text.adventure.AdventureRuntime
import top.likoslupus.cellulosesz.core.text.presentation.TextPresentation
import top.likoslupus.cellulosesz.core.text.presentation.replyNotice

/**
 * `/speed`: set a walk/fly speed multiplier for yourself, or for another player with the
 * `speed.others` node. The change is session-scoped and restored when the player leaves.
 */
internal object SpeedCommands {

    private const val HARD_MINIMUM: Double = 0.0
    private const val HARD_MAXIMUM: Double = 10.0

    fun commands(
        service: PlayerAdjustmentService,
        known: KnownPlayerResolver,
        adventure: AdventureRuntime,
        presentation: TextPresentation,
    ): List<CommandDefinition> = listOf(
        command(
            name = "speed",
            category = CommandCategory.ADMINISTRATION,
            permission = CommandPermissions.SPEED,
            documentation = "administration/speed",
        ) {
            argument(
                "multiplier",
                double(min = HARD_MINIMUM, max = HARD_MAXIMUM)
            ) { multiplier ->
                executesPlayer {
                    context.source.replyNotice(
                        adventure,
                        presentation,
                        playerId,
                        AdministrationMessages.speed(
                            service.setSpeed(
                                playerId,
                                get(multiplier)
                            )
                        ),
                    )
                }
                argument(
                    "player",
                    word(),
                    permission = CommandPermissions.SPEED_OTHERS
                ) { player ->
                    suggests { suggestOnlinePlayers() }
                    executes {
                        when (val id = known.onlineByName(get(player))?.id) {
                            null -> context.source.replyNotice(
                                adventure,
                                presentation,
                                context.source.player?.uuid,
                                AdministrationMessages.notFound(),
                            )

                            else -> context.source.replyNotice(
                                adventure,
                                presentation,
                                context.source.player?.uuid,
                                AdministrationMessages.speed(
                                    service.setSpeed(
                                        id,
                                        get(multiplier)
                                    )
                                ),
                            )
                        }
                    }
                }
            }
        },
    )

    private fun SuggestionScope.suggestOnlinePlayers() {
        source.server.playerList.players.forEach {
            suggest(it.gameProfile.name)
        }
    }

}
