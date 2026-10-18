package top.likoslupus.cellulosesz.utility.itemtools

import com.mojang.brigadier.context.CommandContext
import net.minecraft.commands.CommandSourceStack
import top.likoslupus.cellulosesz.core.command.CommandCategory
import top.likoslupus.cellulosesz.core.command.dsl.*
import top.likoslupus.cellulosesz.core.permission.CommandPermissions
import top.likoslupus.cellulosesz.core.text.adventure.AdventureRuntime
import top.likoslupus.cellulosesz.core.text.presentation.*
import top.likoslupus.cellulosesz.utility.format.*
import java.util.*

internal object ItemGrantCommands {

    fun commands(
        service: ItemToolsService,
        adventure: AdventureRuntime,
        presentation: TextPresentation,
    ): List<CommandDefinition> = listOf(
        command(
            name = "give",
            category = CommandCategory.UTILITY,
            permission = CommandPermissions.GIVE,
            documentation = "utility/give",
        ) {
            argument("player", word()) { player ->
                suggests { source.server.playerList.players.forEach { suggest(it.gameProfile.name) } }
                argument("item", item()) { item ->
                    executes {
                        give(
                            context,
                            get(player),
                            get(item),
                            null,
                            service,
                            adventure,
                            presentation
                        )
                    }
                    argument("amount", integer(min = 1)) { amount ->
                        executes {
                            give(
                                context,
                                get(player),
                                get(item),
                                get(amount),
                                service,
                                adventure,
                                presentation
                            )
                        }
                    }
                }
            }
        },

        command(
            name = "item",
            category = CommandCategory.UTILITY,
            permission = CommandPermissions.ITEM,
            documentation = "utility/item",
        ) {
            argument("item", item()) { item ->
                executesPlayer {
                    grantItem(
                        context,
                        playerId,
                        get(item),
                        null,
                        service,
                        adventure,
                        presentation
                    )
                }
                argument("amount", integer(min = 1)) { amount ->
                    executesPlayer {
                        grantItem(
                            context,
                            playerId,
                            get(item),
                            get(amount),
                            service,
                            adventure,
                            presentation
                        )
                    }
                }
            }
        },

        command(
            name = "itemdb",
            category = CommandCategory.UTILITY,
            permission = CommandPermissions.ITEM_DB,
            documentation = "utility/itemdb",
        ) {
            executesPlayer {
                itemDb(
                    context,
                    playerId,
                    null,
                    service,
                    adventure,
                    presentation
                )
            }
            argument("item", item()) { item ->
                executesPlayer {
                    itemDb(
                        context,
                        playerId,
                        get(item),
                        service,
                        adventure,
                        presentation
                    )
                }
            }
        },
    )

    private fun give(
        context: CommandContext<CommandSourceStack>,
        targetName: String,
        input: net.minecraft.commands.arguments.item.ItemInput,
        amount: Int?,
        service: ItemToolsService,
        adventure: AdventureRuntime,
        presentation: TextPresentation,
    ): Int {
        val source = context.source
        return source.replyNotices(
            adventure,
            presentation,
            source.player?.uuid,
            UtilityMessages.give(service.give(targetName, input, amount)),
        )
    }

    private fun grantItem(
        context: CommandContext<CommandSourceStack>,
        actorId: UUID,
        input: net.minecraft.commands.arguments.item.ItemInput,
        amount: Int?,
        service: ItemToolsService,
        adventure: AdventureRuntime,
        presentation: TextPresentation,
    ): Int {
        val source = context.source
        return source.replyNotices(
            adventure,
            presentation,
            actorId,
            UtilityMessages.give(service.item(actorId, input, amount)),
        )
    }

    private fun itemDb(
        context: CommandContext<CommandSourceStack>,
        actorId: UUID,
        input: net.minecraft.commands.arguments.item.ItemInput?,
        service: ItemToolsService,
        adventure: AdventureRuntime,
        presentation: TextPresentation,
    ): Int {
        val source = context.source
        val info = service.itemInfo(actorId, input)
            ?: return source.replyNotice(
                adventure,
                presentation,
                actorId,
                UtilityMessages.itemInfoMissing(),
            )
        return source.replyDetail(
            adventure,
            presentation,
            actorId,
            UtilityMessages.itemInfo(info)
        )
    }

}
