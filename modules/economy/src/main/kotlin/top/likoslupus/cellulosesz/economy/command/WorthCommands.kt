package top.likoslupus.cellulosesz.economy.command

import net.minecraft.commands.CommandSourceStack
import net.minecraft.commands.arguments.item.ItemInput
import net.minecraft.core.registries.BuiltInRegistries
import net.minecraft.server.level.ServerPlayer
import top.likoslupus.cellulosesz.core.command.CommandCategory
import top.likoslupus.cellulosesz.core.command.dsl.CommandDefinition
import top.likoslupus.cellulosesz.core.command.dsl.command
import top.likoslupus.cellulosesz.core.command.dsl.item
import top.likoslupus.cellulosesz.core.command.dsl.word
import top.likoslupus.cellulosesz.core.permission.CommandPermissions
import top.likoslupus.cellulosesz.core.text.MessageKey
import top.likoslupus.cellulosesz.core.text.adventure.replyError
import top.likoslupus.cellulosesz.core.text.presentation.replyNotice
import top.likoslupus.cellulosesz.core.text.presentation.replyNotices
import top.likoslupus.cellulosesz.economy.format.EconomyMessages
import top.likoslupus.cellulosesz.economy.money.Money
import top.likoslupus.cellulosesz.economy.sale.CountParsing
import top.likoslupus.cellulosesz.economy.sale.SaleScope
import java.math.BigDecimal
import java.util.*

/** `/worth` and its `hand`/`inventory`/`blocks`/`item` forms. */
internal object WorthCommands {

    fun definition(context: WorthSellContext): CommandDefinition = command(
        name = "worth",
        category = CommandCategory.ECONOMY,
        permission = CommandPermissions.WORTH,
        documentation = "economy/worth",
    ) {
        executesPlayer {
            worthHeld(
                context,
                source,
                playerId,
                null
            )
        }
        literal("hand") {
            executesPlayer {
                worthHeld(
                    context,
                    source,
                    playerId,
                    null
                )
            }
            argument("amount", word()) { amount ->
                executesPlayer {
                    worthHeld(
                        context,
                        source,
                        playerId,
                        get(amount)
                    )
                }
            }
        }
        literal("inventory") {
            executesPlayer { worthInventory(context, source, playerId) }
        }
        literal("blocks") {
            executesPlayer {
                worthBlocks(
                    context,
                    source,
                    playerId,
                    null
                )
            }
            argument("amount", word()) { amount ->
                executesPlayer {
                    worthBlocks(
                        context,
                        source,
                        playerId,
                        get(amount)
                    )
                }
            }
        }
        argument("item", item()) { itemKey ->
            executesPlayer {
                worthItem(
                    context,
                    source,
                    playerId,
                    itemIdOf(get(itemKey)),
                    null
                )
            }
            argument("amount", word()) { amount ->
                executesPlayer {
                    worthItem(
                        context,
                        source,
                        playerId,
                        itemIdOf(get(itemKey)),
                        get(amount)
                    )
                }
            }
        }
    }

    private fun worthHeld(
        context: WorthSellContext,
        source: CommandSourceStack,
        playerId: UUID,
        amountText: String?,
    ): Int {
        if (!context.settings().enabled) {
            return context.disabled(source, playerId)
        }
        val amount = when (amountText) {
            null -> null
            else -> CountParsing.signed(amountText)
                ?: return invalidAmount(context, source, playerId)
        }
        val slot = context.inventory.scan(
            playerId,
            SaleScope.HAND,
            null
        ).firstOrNull()
            ?: return source.replyNotice(
                context.adventure,
                context.presentation,
                playerId,
                EconomyMessages.worthHeldEmpty(),
            )
        val price = context.worth.unitPrice(slot.itemId)
            ?: return source.replyNotice(
                context.adventure,
                context.presentation,
                playerId,
                EconomyMessages.worthMissing(slot.itemId),
            )
        return replyWorthUnit(
            context,
            source,
            playerId,
            slot.itemId,
            price,
            amount ?: slot.count
        )
    }

    private fun worthItem(
        context: WorthSellContext,
        source: CommandSourceStack,
        playerId: UUID,
        itemId: String?,
        amountText: String?,
    ): Int {
        if (!context.settings().enabled) {
            return context.disabled(source, playerId)
        }
        val amount = when (amountText) {
            null -> null
            else -> CountParsing.signed(amountText)
                ?: return invalidAmount(context, source, playerId)
        }
        val resolved = itemId
            ?: return source.replyError(
                context.adventure,
                context.messages.renderFor(
                    playerId,
                    MessageKey("economy.worth.unknown_item")
                ),
            )
        val owned = context.inventory.scan(
            playerId,
            SaleScope.ITEM,
            resolved
        ).sumOf { it.count }
        val price = context.worth.unitPrice(resolved)
            ?: return source.replyNotice(
                context.adventure,
                context.presentation,
                playerId,
                EconomyMessages.worthMissing(resolved),
            )
        return replyWorthUnit(
            context,
            source,
            playerId,
            resolved,
            price,
            amount ?: owned
        )
    }

    private fun worthInventory(
        context: WorthSellContext,
        source: CommandSourceStack,
        playerId: UUID,
    ): Int {
        if (!context.settings().enabled) {
            return context.disabled(source, playerId)
        }
        val slots = context.inventory.scan(
            playerId,
            SaleScope.INVENTORY,
            null
        )
        var total = BigDecimal.ZERO
        var items = 0
        slots.forEach { slot ->
            val price = context.worth.unitPrice(slot.itemId)
                ?: return@forEach
            total = total.add(
                price.decimal.multiply(
                    BigDecimal(slot.count)
                )
            )
            items += slot.count
        }
        return when {
            items == 0 -> source.replyNotice(
                context.adventure,
                context.presentation,
                playerId,
                EconomyMessages.worthNothing(),
            )

            else -> source.replyNotice(
                context.adventure,
                context.presentation,
                playerId,
                EconomyMessages.worthInventory(
                    total,
                    items,
                    context.display()
                ),
            )
        }
    }

    private fun worthBlocks(
        context: WorthSellContext,
        source: CommandSourceStack,
        playerId: UUID,
        amountText: String?,
    ): Int {
        if (!context.settings().enabled) {
            return context.disabled(source, playerId)
        }
        val cap = when (amountText) {
            null -> null
            else -> CountParsing.signed(amountText)
                    ?.takeIf { it > 0 }
                ?: return invalidAmount(context, source, playerId)
        }
        val slots = context.inventory.scan(
            playerId,
            SaleScope.BLOCKS,
            null
        )
        var total = BigDecimal.ZERO
        var items = 0
        slots.forEach { slot ->
            if (cap != null && items >= cap) {
                return@forEach
            }
            val price = context.worth.unitPrice(slot.itemId)
                ?: return@forEach
            val count = when (cap) {
                null -> slot.count
                else -> minOf(slot.count, cap - items)
            }
            total = total.add(price.decimal.multiply(BigDecimal(count)))
            items += count
        }
        return when {
            items == 0 -> source.replyNotice(
                context.adventure,
                context.presentation,
                playerId,
                EconomyMessages.worthNothing(),
            )

            else -> source.replyNotice(
                context.adventure,
                context.presentation,
                playerId,
                EconomyMessages.worthBlocks(
                    total,
                    items,
                    context.display()
                ),
            )
        }
    }

    private fun replyWorthUnit(
        context: WorthSellContext,
        source: CommandSourceStack,
        playerId: UUID,
        itemId: String,
        price: Money,
        count: Int,
    ): Int {
        val total = price.decimal.multiply(BigDecimal(count))
        return source.replyNotices(
            context.adventure,
            context.presentation,
            playerId,
            listOf(
                EconomyMessages.worthUnit(
                    itemId,
                    price.decimal,
                    context.display()
                ),
                EconomyMessages.worthTotal(
                    itemId,
                    count,
                    total,
                    context.display()
                ),
            ),
        )
    }

}

internal fun itemIdOf(input: ItemInput?): String? {
    val resolved = input ?: return null
    return resolved.item().unwrapKey()
            .map { it.identifier().toString() }
            .orElse(null)
}

internal fun itemIdOf(player: ServerPlayer?): String? =
    player?.mainHandItem?.takeIf { !it.isEmpty }?.let {
        BuiltInRegistries.ITEM.getKey(it.item).toString()
    }
