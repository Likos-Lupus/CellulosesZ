package top.likoslupus.cellulosesz.utility.item

import com.mojang.brigadier.context.CommandContext
import net.minecraft.commands.CommandSourceStack
import top.likoslupus.cellulosesz.core.command.CommandCategory
import top.likoslupus.cellulosesz.core.command.dsl.CommandDefinition
import top.likoslupus.cellulosesz.core.command.dsl.command
import top.likoslupus.cellulosesz.core.command.dsl.integer
import top.likoslupus.cellulosesz.core.command.dsl.word
import top.likoslupus.cellulosesz.core.command.reply
import top.likoslupus.cellulosesz.core.command.replyError
import top.likoslupus.cellulosesz.core.permission.CommandPermissions
import top.likoslupus.cellulosesz.core.player.KnownPlayerResolver
import top.likoslupus.cellulosesz.utility.format.UtilityMessages
import java.util.*

internal object ItemUtilityCommands {

    fun commands(
        service: ItemUtilityService,
        known: KnownPlayerResolver,
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
                    known
                )
            }
            literal("hand") {
                executes {
                    repair(
                        context,
                        RepairScope.HAND,
                        null,
                        service,
                        known
                    )
                }
                argument("player", word()) { player ->
                    suggests {
                        source.server.playerList.players.forEach {
                            suggest(it.gameProfile.name)
                        }
                    }
                    executes {
                        repair(
                            context,
                            RepairScope.HAND,
                            get(player),
                            service,
                            known
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
                        known
                    )
                }
                argument("player", word()) { player ->
                    suggests {
                        source.server.playerList.players.forEach {
                            suggest(it.gameProfile.name)
                        }
                    }
                    executes {
                        repair(
                            context,
                            RepairScope.ALL,
                            get(player),
                            service,
                            known
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
                    service
                )
            }
            argument("amount", integer(min = 1)) { amount ->
                executes {
                    more(
                        context,
                        get(amount),
                        service
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
            executes { condense(context, service) }
        },
    )

    private fun repair(
        context: CommandContext<CommandSourceStack>,
        scope: RepairScope,
        rawTarget: String?,
        service: ItemUtilityService,
        known: KnownPlayerResolver,
    ): Int =
        context.source.let {
            when (val targetId = resolveTarget(it.player?.uuid, rawTarget, known)) {
                null -> it.replyError(UtilityMessages.prefixed("player not found or offline"))
                else -> it.reply(UtilityMessages.repair(service.repair(targetId, scope)))
            }
        }

    private fun more(
        context: CommandContext<CommandSourceStack>,
        amount: Int?,
        service: ItemUtilityService,
    ): Int =
        context.source.let {
            when (val playerId = it.player?.uuid) {
                null -> it.replyError(UtilityMessages.requiresPlayer())
                else -> it.reply(UtilityMessages.more(service.more(playerId, amount)))
            }
        }

    private fun condense(
        context: CommandContext<CommandSourceStack>,
        service: ItemUtilityService,
    ): Int =
        context.source.let {
            when (val playerId = it.player?.uuid) {
                null -> it.replyError(UtilityMessages.requiresPlayer())
                else -> it.reply(UtilityMessages.condense(service.condense(playerId)))
            }
        }

    private fun resolveTarget(
        selfId: UUID?,
        rawTarget: String?,
        known: KnownPlayerResolver,
    ): UUID? =
        when (rawTarget) {
            null -> selfId
            else -> known.onlineByName(rawTarget)?.id
        }

}
