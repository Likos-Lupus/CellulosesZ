package top.likoslupus.cellulosesz.administration.playerstate

import top.likoslupus.cellulosesz.administration.format.*
import top.likoslupus.cellulosesz.core.command.CommandCategory
import top.likoslupus.cellulosesz.core.command.dsl.*
import top.likoslupus.cellulosesz.core.permission.CommandPermissions
import top.likoslupus.cellulosesz.core.player.KnownPlayerResolver
import top.likoslupus.cellulosesz.core.text.adventure.AdventureRuntime
import top.likoslupus.cellulosesz.core.text.presentation.TextPresentation
import top.likoslupus.cellulosesz.core.text.presentation.replyDetail
import top.likoslupus.cellulosesz.core.text.presentation.replyNotice
import java.util.*

/**
 * `/exp`: view, give, take and set experience points. Viewing your own experience is a player
 * command; every other form needs its own moderator child node.
 */
internal object ExperienceCommands {

    fun commands(
        service: PlayerAdjustmentService,
        known: KnownPlayerResolver,
        adventure: AdventureRuntime,
        presentation: TextPresentation,
    ): List<CommandDefinition> = listOf(
        command(
            name = "exp",
            category = CommandCategory.ADMINISTRATION,
            permission = CommandPermissions.EXP,
            documentation = "administration/exp",
        ) {
            executesPlayer {
                replyExperience(
                    context.source,
                    playerId,
                    service.viewExperience(playerId),
                    adventure,
                    presentation,
                )
            }

            literal("get") {
                executesPlayer {
                    replyExperience(
                        context.source,
                        playerId,
                        service.viewExperience(playerId),
                        adventure,
                        presentation,
                    )
                }

                argument("player", word(), permission = CommandPermissions.EXP_GET) { player ->
                    suggests { suggestOnlinePlayers() }
                    executes {
                        val id = known.onlineByName(get(player))?.id
                        if (id == null) {
                            replyNotFound(context.source, adventure, presentation)
                        } else {
                            replyExperience(
                                context.source,
                                context.source.player?.uuid,
                                service.viewExperience(id),
                                adventure,
                                presentation,
                            )
                        }
                    }
                }
            }

            literal("give", permission = CommandPermissions.EXP_GIVE) {
                adjustment(
                    operation = ExperienceOperation.GIVE,
                    minimum = 1,
                    service = service,
                    known = known,
                    adventure = adventure,
                    presentation = presentation,
                )
            }

            literal("take", permission = CommandPermissions.EXP_TAKE) {
                adjustment(
                    operation = ExperienceOperation.TAKE,
                    minimum = 1,
                    service = service,
                    known = known,
                    adventure = adventure,
                    presentation = presentation,
                )
            }

            literal("set", permission = CommandPermissions.EXP_SET) {
                adjustment(
                    operation = ExperienceOperation.SET,
                    minimum = 0,
                    service = service,
                    known = known,
                    adventure = adventure,
                    presentation = presentation,
                )
            }
        },
    )

    private fun CommandSyntax.adjustment(
        operation: ExperienceOperation,
        minimum: Int,
        service: PlayerAdjustmentService,
        known: KnownPlayerResolver,
        adventure: AdventureRuntime,
        presentation: TextPresentation,
    ) {
        argument("amount", integer(min = minimum)) { amount ->
            executesPlayer {
                context.source.replyNotice(
                    adventure,
                    presentation,
                    playerId,
                    AdministrationMessages.experienceChange(
                        service.adjustExperience(
                            playerId,
                            operation,
                            get(amount)
                        )
                    ),
                )
            }
            argument("player", word()) { player ->
                suggests { suggestOnlinePlayers() }
                executes {
                    when (val id = known.onlineByName(get(player))?.id) {
                        null -> replyNotFound(context.source, adventure, presentation)
                        else -> context.source.replyNotice(
                            adventure,
                            presentation,
                            context.source.player?.uuid,
                            AdministrationMessages.experienceChange(
                                service.adjustExperience(
                                    id,
                                    operation,
                                    get(amount)
                                )
                            ),
                        )
                    }
                }
            }
        }
    }

    private fun replyExperience(
        source: net.minecraft.commands.CommandSourceStack,
        viewerId: UUID?,
        result: ExperienceResult,
        adventure: AdventureRuntime,
        presentation: TextPresentation,
    ): Int =
        when (result) {
            is ExperienceResult.Viewed -> source.replyDetail(
                adventure,
                presentation,
                viewerId,
                AdministrationMessages.experienceView(result),
            )

            else -> replyNotFound(source, adventure, presentation)
        }

    private fun replyNotFound(
        source: net.minecraft.commands.CommandSourceStack,
        adventure: AdventureRuntime,
        presentation: TextPresentation,
    ): Int =
        source.replyNotice(
            adventure,
            presentation,
            source.player?.uuid,
            AdministrationMessages.notFound(),
        )

    private fun SuggestionScope.suggestOnlinePlayers() {
        source.server.playerList.players.forEach {
            suggest(it.gameProfile.name)
        }
    }

}
