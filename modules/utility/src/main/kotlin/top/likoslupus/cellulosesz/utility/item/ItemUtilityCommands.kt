package top.likoslupus.cellulosesz.utility.item

import com.mojang.brigadier.context.CommandContext
import net.minecraft.commands.CommandSourceStack
import top.likoslupus.cellulosesz.core.command.CommandCategory
import top.likoslupus.cellulosesz.core.command.dsl.CommandDefinition
import top.likoslupus.cellulosesz.core.command.dsl.command
import top.likoslupus.cellulosesz.core.command.dsl.integer
import top.likoslupus.cellulosesz.core.command.dsl.word
import top.likoslupus.cellulosesz.core.permission.CommandPermissions
import top.likoslupus.cellulosesz.core.player.KnownPlayerResolver
import top.likoslupus.cellulosesz.core.text.MessageKey
import top.likoslupus.cellulosesz.core.text.adventure.AdventureRuntime
import top.likoslupus.cellulosesz.core.text.adventure.replyError
import top.likoslupus.cellulosesz.core.text.i18n.LocalizedMessages
import top.likoslupus.cellulosesz.core.text.presentation.TextPresentation
import top.likoslupus.cellulosesz.core.text.presentation.replyNotice
import top.likoslupus.cellulosesz.core.text.presentation.replyNotices
import top.likoslupus.cellulosesz.utility.format.UtilityMessages

internal object ItemUtilityCommands {

    fun commands(
        service: ItemUtilityService,
        known: KnownPlayerResolver,
        messages: LocalizedMessages,
        adventure: AdventureRuntime,
        presentation: TextPresentation,
    ): List<CommandDefinition> = listOf(
        command(
            name = "repair",
            category = CommandCategory.UTILITY,
            permission = CommandPermissions.REPAIR,
            documentation = "utility/repair",
        ) {
            executes {
                repair(
                    context,
                    RepairScope.HAND,
                    null,
                    service,
                    known,
                    messages,
                    adventure,
                    presentation
                )
            }
            literal("hand") {
                executes {
                    repair(
                        context,
                        RepairScope.HAND,
                        null,
                        service,
                        known,
                        messages,
                        adventure,
                        presentation
                    )
                }
                argument("player", word()) { player ->
                    suggests {
                        source.server.playerList.players.forEach { suggest(it.gameProfile.name) }
                    }
                    executes {
                        repair(
                            context,
                            RepairScope.HAND,
                            get(player),
                            service,
                            known,
                            messages,
                            adventure,
                            presentation
                        )
                    }
                }
            }
            literal("all") {
                executes {
                    repair(
                        context,
                        RepairScope.ALL,
                        null,
                        service,
                        known,
                        messages,
                        adventure,
                        presentation
                    )
                }
                argument("player", word()) { player ->
                    suggests {
                        source.server.playerList.players.forEach { suggest(it.gameProfile.name) }
                    }
                    executes {
                        repair(
                            context,
                            RepairScope.ALL,
                            get(player),
                            service,
                            known,
                            messages,
                            adventure,
                            presentation
                        )
                    }
                }
            }
        },

        command(
            name = "more",
            category = CommandCategory.UTILITY,
            permission = CommandPermissions.MORE,
            documentation = "utility/more",
        ) {
            executes {
                more(
                    context,
                    null,
                    service,
                    messages,
                    adventure,
                    presentation
                )
            }
            argument("amount", integer(min = 1)) { amount ->
                executes {
                    more(
                        context,
                        get(amount),
                        service,
                        messages,
                        adventure,
                        presentation
                    )
                }
            }
        },

        command(
            name = "condense",
            category = CommandCategory.UTILITY,
            permission = CommandPermissions.CONDENSE,
            documentation = "utility/condense",
        ) {
            executes {
                condense(
                    context,
                    service,
                    messages,
                    adventure,
                    presentation
                )
            }
        },
    )

    private fun repair(
        context: CommandContext<CommandSourceStack>,
        scope: RepairScope,
        rawTarget: String?,
        service: ItemUtilityService,
        known: KnownPlayerResolver,
        messages: LocalizedMessages,
        adventure: AdventureRuntime,
        presentation: TextPresentation,
    ): Int {
        val source = context.source
        val executorId = source.player?.uuid
        val targetId = when (rawTarget) {
            null -> executorId
            else -> known.onlineByName(rawTarget)?.id
        } ?: return source.replyError(
            adventure,
            messages.renderFor(
                executorId,
                MessageKey("utility.error.player_not_found")
            )
        )
        return source.replyNotices(
            adventure,
            presentation,
            executorId,
            UtilityMessages.repair(
                service.repair(targetId, scope)
            )
        )
    }

    private fun more(
        context: CommandContext<CommandSourceStack>,
        amount: Int?,
        service: ItemUtilityService,
        messages: LocalizedMessages,
        adventure: AdventureRuntime,
        presentation: TextPresentation,
    ): Int {
        val source = context.source
        val playerId = source.player?.uuid
            ?: return source.replyError(
                adventure,
                messages.renderFor(
                    null,
                    MessageKey("core.error.player_required")
                )
            )
        return source.replyNotice(
            adventure,
            presentation,
            playerId,
            UtilityMessages.more(
                service.more(playerId, amount)
            )
        )
    }

    private fun condense(
        context: CommandContext<CommandSourceStack>,
        service: ItemUtilityService,
        messages: LocalizedMessages,
        adventure: AdventureRuntime,
        presentation: TextPresentation,
    ): Int {
        val source = context.source
        val playerId = source.player?.uuid
            ?: return source.replyError(
                adventure,
                messages.renderFor(
                    null,
                    MessageKey("core.error.player_required")
                )
            )
        return source.replyNotice(
            adventure,
            presentation,
            playerId,
            UtilityMessages.condense(
                service.condense(playerId)
            )
        )
    }

}
