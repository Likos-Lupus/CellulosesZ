package top.likoslupus.cellulosesz.economy.sale

import net.minecraft.core.registries.BuiltInRegistries
import net.minecraft.server.level.ServerPlayer
import net.minecraft.world.item.BlockItem
import net.minecraft.world.item.ItemStack
import top.likoslupus.cellulosesz.core.player.PlayerResolver
import top.likoslupus.cellulosesz.core.runtime.RuntimeKernel
import java.util.*

/** What part of an inventory a sale looks at. */
internal enum class SaleScope {

    HAND,
    ITEM,
    INVENTORY,
    BLOCKS,

}

/** One scannable inventory slot with everything the money math needs. */
internal data class InventorySlot(
    val slot: Int,
    val itemId: String,
    val count: Int,
    val customNamed: Boolean,
    val block: Boolean,
)

/**
 * The only class that reads or mutates a player's items for economy. Every method is server-thread
 * confined and resolves the player fresh from the live server, so no [ServerPlayer] is ever held
 * across a suspension. Removal re-validates each slot first: a changed inventory aborts the sale with
 * nothing taken.
 */
internal class SaleInventory(
    private val kernel: RuntimeKernel,
    private val codec: SaleItemCodec,
) {

    /** Current slots matching [scope]; [itemId] narrows [SaleScope.ITEM]. Server thread only. */
    fun scan(
        playerId: UUID,
        scope: SaleScope,
        itemId: String?
    ): List<InventorySlot> {
        val player = online(playerId) ?: return emptyList()
        return when (scope) {
            SaleScope.HAND -> {
                val slot = player.inventory.selectedSlot
                val stack = player.inventory.getItem(slot)
                slotOf(slot, stack)?.let(::listOf) ?: emptyList()
            }

            SaleScope.ITEM -> selectable(player).filter { it.itemId == itemId }

            SaleScope.INVENTORY -> selectable(player)

            SaleScope.BLOCKS -> selectable(player).filter { it.block }
        }
    }

    /**
     * Copies the exact stacks a plan will remove *without* removing them, so the intent payload can
     * be written before the items leave the inventory. Returns fewer entries than [lines] when a slot
     * no longer matches; the caller then aborts before persisting anything.
     */
    fun snapshot(playerId: UUID, lines: List<SellLine>): List<RemovedStack> {
        val player = online(playerId) ?: return emptyList()
        return lines.mapNotNull { line ->
            val stack = player.inventory.getItem(line.slot)
            if (stack.isEmpty || itemIdOf(stack) != line.itemId) {
                return@mapNotNull null
            }
            RemovedStack(
                line.slot,
                stack.copyWithCount(minOf(line.count, stack.count))
            )
        }
    }

    /**
     * Removes exactly [lines] from the player's inventory, returning the removed stacks (or null when
     * any slot no longer holds the planned item and count, in which case nothing is removed).
     */
    fun remove(playerId: UUID, lines: List<SellLine>): List<RemovedStack>? {
        if (lines.isEmpty()) return emptyList()
        val player = online(playerId) ?: return null
        val current = lines.map { player.inventory.getItem(it.slot) }

        lines.forEachIndexed { index, line ->
            val stack = current[index]
            if (stack.isEmpty
                || stack.count < line.count
                || itemIdOf(stack) != line.itemId
            ) {
                return null
            }
        }

        val removed = lines.mapIndexed { index, line ->
            val stack = current[index]
            val remaining = stack.count - line.count
            player.inventory.setItem(
                line.slot,
                if (remaining == 0) ItemStack.EMPTY else stack.copyWithCount(remaining),
            )
            RemovedStack(line.slot, stack.copyWithCount(line.count))
        }
        player.inventory.setChanged()
        return removed
    }

    /** Restores the exact stacks of a `TAKEN` intent. False when the player is offline. */
    fun restore(playerId: UUID, payload: String): Boolean {
        val player = online(playerId) ?: return false
        val stacks = try {
            codec.decode(payload)
        } catch (_: Exception) {
            // An unreadable payload cannot be restored; treat the record as settled rather than
            // looping forever. Items may be lost — money is never affected.
            return true
        }
        stacks.forEach { player.inventory.placeItemBackInInventory(it.stack) }
        player.inventory.setChanged()
        return true
    }

    private fun online(playerId: UUID): ServerPlayer? =
        PlayerResolver.onlineById(kernel.requireServer(), playerId)

    private fun selectable(player: ServerPlayer): List<InventorySlot> =
        player.inventory.nonEquipmentItems
                .mapIndexedNotNull { index, stack -> slotOf(index, stack) }

    private fun slotOf(slot: Int, stack: ItemStack): InventorySlot? {
        if (stack.isEmpty) return null
        return InventorySlot(
            slot = slot,
            itemId = itemIdOf(stack),
            count = stack.count,
            customNamed = stack.customName != null,
            block = stack.item is BlockItem,
        )
    }

    private fun itemIdOf(stack: ItemStack): String =
        BuiltInRegistries.ITEM.getKey(stack.item).toString()

}
