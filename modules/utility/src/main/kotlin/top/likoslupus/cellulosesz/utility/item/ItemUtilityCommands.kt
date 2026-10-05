package top.likoslupus.cellulosesz.utility.item

import com.mojang.brigadier.CommandDispatcher
import com.mojang.brigadier.arguments.IntegerArgumentType.getInteger
import com.mojang.brigadier.arguments.IntegerArgumentType.integer
import com.mojang.brigadier.arguments.StringArgumentType
import com.mojang.brigadier.context.CommandContext
import net.minecraft.commands.CommandSourceStack
import net.minecraft.commands.Commands.argument
import net.minecraft.commands.Commands.literal
import top.likoslupus.cellulosesz.core.command.canUseModeratorCommands
import top.likoslupus.cellulosesz.core.command.reply
import top.likoslupus.cellulosesz.core.command.replyError
import top.likoslupus.cellulosesz.core.player.KnownPlayerResolver
import top.likoslupus.cellulosesz.utility.format.UtilityMessages
import java.util.*

internal object ItemUtilityCommands {

    fun register(
        dispatcher: CommandDispatcher<CommandSourceStack>,
        service: ItemUtilityService,
        known: KnownPlayerResolver,
    ) {
        dispatcher.register(
            literal("repair")
                    .requires { it.canUseModeratorCommands() }
                    .executes { context ->
                        repair(
                            context,
                            RepairScope.HAND,
                            null,
                            service,
                            known
                        )
                    }
                    .then(
                        literal("hand")
                                .executes { context ->
                                    repair(
                                        context,
                                        RepairScope.HAND,
                                        null,
                                        service,
                                        known
                                    )
                                }
                                .then(
                                    argument("player", StringArgumentType.word())
                                            .suggests { context, builder ->
                                                context.source.server.playerList.players
                                                        .forEach { builder.suggest(it.gameProfile.name) }
                                                builder.buildFuture()
                                            }
                                            .executes { context ->
                                                repair(
                                                    context,
                                                    RepairScope.HAND,
                                                    StringArgumentType.getString(
                                                        context,
                                                        "player"
                                                    ),
                                                    service,
                                                    known,
                                                )
                                            }
                                )
                    )
                    .then(
                        literal("all")
                                .executes { context ->
                                    repair(
                                        context,
                                        RepairScope.ALL,
                                        null,
                                        service,
                                        known
                                    )
                                }
                                .then(
                                    argument("player", StringArgumentType.word())
                                            .suggests { context, builder ->
                                                context.source.server.playerList.players
                                                        .forEach { builder.suggest(it.gameProfile.name) }
                                                builder.buildFuture()
                                            }
                                            .executes { context ->
                                                repair(
                                                    context,
                                                    RepairScope.ALL,
                                                    StringArgumentType.getString(
                                                        context,
                                                        "player"
                                                    ),
                                                    service,
                                                    known,
                                                )
                                            }
                                )
                    )
        )
        dispatcher.register(
            literal("more")
                    .requires { it.canUseModeratorCommands() }
                    .executes { context -> more(context, null, service) }
                    .then(
                        argument("amount", integer(1))
                                .executes { context ->
                                    more(
                                        context,
                                        getInteger(
                                            context,
                                            "amount"
                                        ),
                                        service,
                                    )
                                }
                    )
        )
        dispatcher.register(
            literal("condense")
                    .executes { context -> condense(context, service) }
        )
    }

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
                else -> it.reply(
                    UtilityMessages.repair(
                        service.repair(
                            targetId,
                            scope
                        )
                    )
                )
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
                else -> it.reply(
                    UtilityMessages.more(
                        service.more(
                            playerId,
                            amount
                        )
                    )
                )
            }
        }

    private fun condense(
        context: CommandContext<CommandSourceStack>,
        service: ItemUtilityService,
    ): Int =
        context.source.let {
            when (val playerId = it.player?.uuid) {
                null -> it.replyError(UtilityMessages.requiresPlayer())
                else -> it.reply(
                    UtilityMessages.condense(
                        service.condense(
                            playerId
                        )
                    )
                )
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
