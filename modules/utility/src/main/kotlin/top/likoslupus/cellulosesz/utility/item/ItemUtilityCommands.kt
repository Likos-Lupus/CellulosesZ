package top.likoslupus.cellulosesz.utility.item

import com.mojang.brigadier.context.CommandContext
import net.minecraft.commands.CommandSourceStack
import top.likoslupus.cellulosesz.core.command.CommandCategory
import top.likoslupus.cellulosesz.core.command.CommandFeedbackTarget
import top.likoslupus.cellulosesz.core.command.dsl.*
import top.likoslupus.cellulosesz.core.permission.CommandPermissions
import top.likoslupus.cellulosesz.core.player.KnownPlayerResolver
import top.likoslupus.cellulosesz.core.runtime.RuntimeKernel
import top.likoslupus.cellulosesz.core.text.MessageKey
import top.likoslupus.cellulosesz.core.text.adventure.AdventureRuntime
import top.likoslupus.cellulosesz.core.text.adventure.replyError
import top.likoslupus.cellulosesz.core.text.i18n.LocalizedMessages
import top.likoslupus.cellulosesz.core.text.model.Notice
import top.likoslupus.cellulosesz.core.text.presentation.TextPresentation
import top.likoslupus.cellulosesz.core.text.presentation.replyNotice
import top.likoslupus.cellulosesz.core.text.presentation.replyNotices
import top.likoslupus.cellulosesz.utility.format.UtilityMessages
import java.util.*

internal object ItemUtilityCommands {

    fun commands(
        service: ItemUtilityService,
        known: KnownPlayerResolver,
        messages: LocalizedMessages,
        adventure: AdventureRuntime,
        presentation: TextPresentation,
        audit: AdministrativeActionAuditPort,
        kernel: RuntimeKernel,
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

        command(
            name = "hat",
            category = CommandCategory.UTILITY,
            permission = CommandPermissions.HAT,
            documentation = "utility/hat",
        ) {
            executesPlayer {
                context.source.replyNotice(
                    adventure,
                    presentation,
                    playerId,
                    UtilityMessages.hat(service.wearHeldItem(playerId)),
                )
            }
        },

        command(
            name = "itemname",
            category = CommandCategory.UTILITY,
            permission = CommandPermissions.ITEM_NAME,
            documentation = "utility/itemname",
        ) {
            executesPlayer {
                context.source.replyNotice(
                    adventure,
                    presentation,
                    playerId,
                    UtilityMessages.itemName(
                        service.setItemName(
                            playerId,
                            null
                        )
                    ),
                )
            }
            argument("name", greedyString()) { name ->
                executesPlayer {
                    context.source.replyNotice(
                        adventure,
                        presentation,
                        playerId,
                        UtilityMessages.itemName(
                            service.setItemName(
                                playerId,
                                get(name)
                            )
                        ),
                    )
                }
            }
        },

        command(
            name = "itemlore",
            category = CommandCategory.UTILITY,
            permission = CommandPermissions.ITEM_LORE,
            documentation = "utility/itemlore",
        ) {
            executesPlayer {
                context.source.replyNotice(
                    adventure,
                    presentation,
                    playerId,
                    UtilityMessages.itemLore(
                        service.setItemLore(
                            playerId,
                            null
                        )
                    ),
                )
            }
            argument("lines", greedyString()) { lines ->
                executesPlayer {
                    context.source.replyNotice(
                        adventure,
                        presentation,
                        playerId,
                        UtilityMessages.itemLore(
                            service.setItemLore(
                                playerId,
                                get(lines)
                            )
                        ),
                    )
                }
            }
        },

        command(
            name = "clearinventory",
            category = CommandCategory.UTILITY,
            permission = CommandPermissions.CLEAR_INVENTORY,
            documentation = "utility/clearinventory",
        ) {
            executesPlayer {
                context.source.replyNotice(
                    adventure,
                    presentation,
                    playerId,
                    UtilityMessages.clearRequest(
                        service.requestInventoryClear(playerId, ClearScope.MAIN)
                    ),
                )
            }
            literal("all") {
                executesPlayer {
                    context.source.replyNotice(
                        adventure,
                        presentation,
                        playerId,
                        UtilityMessages.clearRequest(
                            service.requestInventoryClear(playerId, ClearScope.ALL)
                        ),
                    )
                }
            }
            literal("confirm") {
                argument("code", word()) { code ->
                    executesPlayer {
                        context.source.replyNotice(
                            adventure,
                            presentation,
                            playerId,
                            UtilityMessages.clearInventory(
                                service.confirmInventoryClear(playerId, get(code))
                            ),
                        )
                    }
                }
            }
            literal("target") {
                argument(
                    "player",
                    word(),
                    permission = CommandPermissions.CLEAR_INVENTORY_OTHERS,
                ) { player ->
                    suggests {
                        source.server.playerList.players.forEach { suggest(it.gameProfile.name) }
                    }
                    literal("main") {
                        executesPlayer {
                            requestOther(
                                source,
                                playerId,
                                get(player),
                                ClearScope.MAIN,
                                service,
                                adventure,
                                presentation,
                            )
                        }
                    }
                    literal("all") {
                        executesPlayer {
                            requestOther(
                                source,
                                playerId,
                                get(player),
                                ClearScope.ALL,
                                service,
                                adventure,
                                presentation,
                            )
                        }
                    }
                }
            }
            literal("target-confirm") {
                argument(
                    "player",
                    word(),
                    permission = CommandPermissions.CLEAR_INVENTORY_OTHERS,
                ) { player ->
                    suggests {
                        source.server.playerList.players.forEach { suggest(it.gameProfile.name) }
                    }
                    argument("code", word()) { code ->
                        executesPlayer {
                            confirmOther(
                                source,
                                playerId,
                                get(player),
                                get(code),
                                service,
                                known,
                                adventure,
                                presentation,
                                audit,
                                kernel,
                            )
                        }
                    }
                }
            }
        },
    )

    private fun requestOther(
        source: CommandSourceStack,
        actorId: UUID,
        targetName: String,
        scope: ClearScope,
        service: ItemUtilityService,
        adventure: AdventureRuntime,
        presentation: TextPresentation,
    ): Int {
        val result = service.requestInventoryClearFor(actorId, targetName, scope)
        if (result is ClearRequestResult.Issued && result.targetId != null) {
            notify(
                result.targetId,
                UtilityMessages.clearTargetNotice(result.scope),
                adventure,
                presentation,
            )
        }
        return source.replyNotice(
            adventure,
            presentation,
            actorId,
            UtilityMessages.clearRequest(result),
        )
    }

    private fun confirmOther(
        source: CommandSourceStack,
        actorId: UUID,
        targetName: String,
        code: String,
        service: ItemUtilityService,
        known: KnownPlayerResolver,
        adventure: AdventureRuntime,
        presentation: TextPresentation,
        audit: AdministrativeActionAuditPort,
        kernel: RuntimeKernel,
    ): Int {
        val targetId = known.onlineByName(targetName)?.id
        val result = service.confirmInventoryClearFor(actorId, targetName, code)
        if (targetId != null) {
            kernel.launch {
                audit.record(
                    actorId = actorId,
                    targetId = targetId,
                    targetName = targetName,
                    cleared = result is ClearInventoryResult.Cleared,
                )
            }
        }
        if (result is ClearInventoryResult.Cleared && targetId != null) {
            notify(
                targetId,
                UtilityMessages.clearTargetDone(result.stacks),
                adventure,
                presentation,
            )
        }
        return source.replyNotice(
            adventure,
            presentation,
            actorId,
            UtilityMessages.clearInventory(result),
        )
    }

    private fun notify(
        targetId: UUID,
        notice: Notice,
        adventure: AdventureRuntime,
        presentation: TextPresentation,
    ) {
        presentation.renderFor(targetId, notice).forEach { line ->
            adventure.audience(CommandFeedbackTarget.Player(targetId)).sendMessage(line)
        }
    }

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
