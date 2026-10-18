package top.likoslupus.cellulosesz.utility.kit

import net.minecraft.core.component.DataComponents
import net.minecraft.server.level.ServerPlayer
import net.minecraft.world.entity.EquipmentSlot
import net.minecraft.world.item.ItemStack
import top.likoslupus.cellulosesz.core.player.PlayerResolver
import top.likoslupus.cellulosesz.core.runtime.RuntimeKernel
import top.likoslupus.cellulosesz.utility.inventory.InventoryInsertion
import java.util.*

/**
 * The only class that mutates a player's inventory for kit delivery. Preflight and delivery run the
 * same deterministic insertion simulation over a working copy of the 36 main slots, so an
 * all-or-nothing [KitOverflowPolicy.REJECT] claim can never half-apply.
 */
internal class MinecraftKitInventoryBackend(
    private val kernel: RuntimeKernel,
) : KitInventoryBackend {

    override fun capture(playerId: UUID): KitCaptureResult {
        val player = player(playerId)
            ?: return KitCaptureResult.PlayerOffline
        val items = player.inventory.nonEquipmentItems
                .mapNotNull { stack ->
                    stack.takeUnless { it.isEmpty }?.copy()
                }
        return KitCaptureResult.Captured(items)
    }

    override fun preflight(
        playerId: UUID,
        items: List<ItemStack>,
        overflowPolicy: KitOverflowPolicy,
    ): KitPreflightResult {
        val player = player(playerId)
            ?: return KitPreflightResult.PlayerOffline
        val working = player.mainSlots()
        val leftover = InventoryInsertion.simulate(working, items)
        return when {
            leftover.isEmpty() -> KitPreflightResult.Fits
            overflowPolicy == KitOverflowPolicy.DROP -> KitPreflightResult.Fits
            else -> KitPreflightResult.Full
        }
    }

    override fun deliver(
        playerId: UUID,
        items: List<ItemStack>,
        overflowPolicy: KitOverflowPolicy,
        autoEquip: Boolean,
    ): KitDeliveryResult {
        val player = player(playerId)
            ?: return KitDeliveryResult.PlayerOffline
        val working = player.mainSlots()
        val leftover = InventoryInsertion.simulate(working, items)
        if (overflowPolicy == KitOverflowPolicy.REJECT
            && leftover.isNotEmpty()
        ) {
            return KitDeliveryResult.Failed
        }

        player.inventory.nonEquipmentItems.forEachIndexed { index, _ ->
            player.inventory.setItem(index, working[index])
        }
        player.inventory.setChanged()

        val equipped = when (autoEquip) {
            true -> autoEquip(player)
            false -> 0
        }
        leftover.forEach { player.drop(it, false) }
        return KitDeliveryResult.Delivered(
            droppedStacks = leftover.size,
            equippedStacks = equipped,
        )
    }

    /**
     * Moves delivered armor and offhand items from the main slots into empty equipment slots. Only an
     * empty destination slot is filled; an already-equipped item is never replaced.
     */
    private fun autoEquip(player: ServerPlayer): Int {
        var equipped = 0
        player.inventory.nonEquipmentItems.forEachIndexed { index, stack ->
            if (stack.isEmpty) {
                return@forEachIndexed
            }
            val slot = stack.get(DataComponents.EQUIPPABLE)?.slot
                ?: return@forEachIndexed
            if (slot !in AUTO_EQUIP_SLOTS) {
                return@forEachIndexed
            }
            if (!player.getItemBySlot(slot).isEmpty) {
                return@forEachIndexed
            }
            player.setItemSlot(slot, stack.copy())
            player.inventory.setItem(index, ItemStack.EMPTY)
            equipped++
        }
        if (equipped > 0) {
            player.inventory.setChanged()
        }
        return equipped
    }

    private fun player(playerId: UUID): ServerPlayer? =
        PlayerResolver.onlineById(kernel.requireServer(), playerId)

    private fun ServerPlayer.mainSlots(): MutableList<ItemStack> =
        inventory.nonEquipmentItems
                .map { it.copy() }
                .toMutableList()

    private companion object {

        private val AUTO_EQUIP_SLOTS = setOf(
            EquipmentSlot.HEAD,
            EquipmentSlot.CHEST,
            EquipmentSlot.LEGS,
            EquipmentSlot.FEET,
            EquipmentSlot.OFFHAND,
        )

    }

}
