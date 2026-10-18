package top.likoslupus.cellulosesz.utility.inspection

import net.minecraft.network.chat.Component.literal
import net.minecraft.network.chat.Component.translatable
import net.minecraft.server.level.ServerPlayer
import net.minecraft.world.Container
import net.minecraft.world.InteractionHand
import net.minecraft.world.SimpleContainer
import net.minecraft.world.SimpleMenuProvider
import net.minecraft.world.entity.EquipmentSlot
import net.minecraft.world.entity.player.Inventory
import net.minecraft.world.entity.player.Player
import net.minecraft.world.inventory.ChestMenu
import net.minecraft.world.inventory.MenuType
import net.minecraft.world.item.ItemStack
import top.likoslupus.cellulosesz.core.player.PlayerResolver
import top.likoslupus.cellulosesz.core.runtime.RuntimeKernel
import java.util.*

/**
 * Opens read-only or write-through vanilla chest menus. A read-only `/invsee` snapshots the target
 * inventory into a container whose mutation methods are disabled; an editable `/invsee` and the
 * admin `/enderchest` back the menu with the target's real container.
 */
internal class MinecraftInspectionBackend(
    private val kernel: RuntimeKernel,
) : InspectionBackend {

    override fun openEnderChest(viewerId: UUID, targetId: UUID): InspectionOpenResult {
        val viewer = player(viewerId)
            ?: return InspectionOpenResult.PlayerOffline
        val target = player(targetId)
            ?: return InspectionOpenResult.PlayerOffline
        val title = when (viewerId == targetId) {
            true -> translatable("container.enderchest")
            false -> literal("${target.gameProfile.name} (ender chest)")
        }
        val provider = SimpleMenuProvider(
            { id, inventory, _ ->
                ChestMenu.threeRows(
                    id,
                    inventory,
                    target.enderChestInventory
                )
            },
            title,
        )
        return open(viewer, provider)
    }

    override fun openInventoryView(
        viewerId: UUID,
        targetId: UUID,
        editable: Boolean,
        equip: Boolean,
    ): InspectionOpenResult {
        val viewer = player(viewerId)
            ?: return InspectionOpenResult.PlayerOffline
        val target = player(targetId)
            ?: return InspectionOpenResult.PlayerOffline
        return when {
            equip -> openEquipment(viewer, target)
            editable -> openEditableInventory(viewer, target)
            else -> openReadOnlyInventory(viewer, target)
        }
    }

    override fun openDisposal(playerId: UUID): InspectionOpenResult {
        val player = player(playerId)
            ?: return InspectionOpenResult.PlayerOffline
        val container = SimpleContainer(DISPOSAL_ROWS * 9)
        val provider = SimpleMenuProvider(
            { id, inventory, _ ->
                DisposalMenu(
                    id,
                    inventory,
                    container
                )
            },
            literal("Disposal"),
        )
        return open(player, provider)
    }

    private fun openEditableInventory(
        viewer: ServerPlayer,
        target: ServerPlayer,
    ): InspectionOpenResult {
        val provider = SimpleMenuProvider(
            { id, inventory, _ ->
                ChestMenu(
                    MenuType.GENERIC_9x4,
                    id,
                    inventory,
                    MainInventoryContainer(target),
                    MAIN_ROWS,
                )
            },
            literal("${target.gameProfile.name} (editable)"),
        )
        return open(viewer, provider)
    }

    private fun openEquipment(
        viewer: ServerPlayer,
        target: ServerPlayer,
    ): InspectionOpenResult {
        val provider = SimpleMenuProvider(
            { id, inventory, _ ->
                ChestMenu(
                    MenuType.GENERIC_9x1,
                    id,
                    inventory,
                    EquipmentContainer(target),
                    EQUIP_ROWS,
                )
            },
            literal("${target.gameProfile.name} (equipment)"),
        )
        return open(viewer, provider)
    }

    private fun openReadOnlyInventory(
        viewer: ServerPlayer,
        target: ServerPlayer,
    ): InspectionOpenResult {
        val snapshot = SimpleContainer(INSPECT_ROWS * 9)
        val size = minOf(target.inventory.containerSize, snapshot.containerSize)
        (0 until size).forEach {
            snapshot.setItem(it, target.inventory.getItem(it).copy())
        }
        val readOnly = ReadOnlyContainer(snapshot)
        val title = literal("${target.gameProfile.name} (read-only)")
        val provider = SimpleMenuProvider(
            { id, inventory, _ ->
                InspectMenu(
                    id,
                    inventory,
                    readOnly
                )
            },
            title,
        )
        return open(viewer, provider)
    }

    private fun open(
        player: ServerPlayer,
        provider: SimpleMenuProvider
    ): InspectionOpenResult =
        when {
            player.openMenu(provider).isPresent -> InspectionOpenResult.Opened
            else -> InspectionOpenResult.Failed
        }

    private fun player(playerId: UUID): ServerPlayer? =
        PlayerResolver.onlineById(kernel.requireServer(), playerId)

    private class DisposalMenu(
        containerId: Int,
        inventory: Inventory,
        private val container: Container,
    ) : ChestMenu(
        MenuType.GENERIC_9x3,
        containerId,
        inventory,
        container,
        DISPOSAL_ROWS
    ) {

        override fun removed(player: Player) {
            super.removed(player)
            container.clearContent()
        }

    }

    /** A chest menu that refuses shift-click transfers, complementing the read-only container. */
    private class InspectMenu(
        containerId: Int,
        inventory: Inventory,
        container: Container,
    ) : ChestMenu(
        MenuType.GENERIC_9x5,
        containerId,
        inventory,
        container,
        INSPECT_ROWS
    ) {

        override fun quickMoveStack(
            player: Player,
            slotIndex: Int,
        ): ItemStack =
            ItemStack.EMPTY

    }

    private companion object {

        private const val INSPECT_ROWS: Int = 5
        private const val DISPOSAL_ROWS: Int = 3
        private const val MAIN_ROWS: Int = 4
        private const val EQUIP_ROWS: Int = 1
        private const val MAIN_SLOTS: Int = 36

    }

    /** Write-through view over the target's 36 main inventory slots. */
    private class MainInventoryContainer(
        private val target: ServerPlayer,
    ) : Container {

        override fun getContainerSize(): Int =
            MAIN_SLOTS

        override fun isEmpty(): Boolean =
            target.inventory.nonEquipmentItems.all { it.isEmpty }

        override fun getItem(slot: Int): ItemStack =
            when (slot) {
                in 0 until MAIN_SLOTS -> target.inventory.getItem(slot)
                else -> ItemStack.EMPTY
            }

        override fun removeItem(slot: Int, amount: Int): ItemStack {
            val current = getItem(slot)
            if (current.isEmpty) {
                return ItemStack.EMPTY
            }
            val removed = current.copyWithCount(minOf(amount, current.count))
            setItem(
                slot,
                when {
                    current.count <= amount -> ItemStack.EMPTY
                    else -> current.copyWithCount(current.count - amount)
                },
            )
            return removed
        }

        override fun removeItemNoUpdate(slot: Int): ItemStack {
            val current = getItem(slot)
            setItem(slot, ItemStack.EMPTY)
            return current
        }

        override fun setItem(slot: Int, stack: ItemStack) {
            if (slot in 0 until MAIN_SLOTS) {
                target.inventory.setItem(slot, stack)
            }
        }

        override fun setChanged() =
            target.inventory.setChanged()

        override fun stillValid(player: Player): Boolean =
            true

        override fun clearContent() {
            (0 until MAIN_SLOTS).forEach { setItem(it, ItemStack.EMPTY) }
            setChanged()
        }

    }

    /** Write-through view over the target's armor and offhand slots, exposed as a 9-slot menu. */
    private class EquipmentContainer(
        private val target: ServerPlayer,
    ) : Container {

        override fun getContainerSize(): Int =
            EQUIP_SLOTS

        override fun isEmpty(): Boolean =
            EQUIPMENT_SLOTS.none { !target.getItemBySlot(it).isEmpty }
                    && target.offhandItem.isEmpty

        override fun getItem(slot: Int): ItemStack =
            when (slot) {
                in EQUIPMENT_SLOTS.indices -> target.getItemBySlot(EQUIPMENT_SLOTS[slot])
                EQUIPMENT_SLOTS.size -> target.offhandItem
                else -> ItemStack.EMPTY
            }

        override fun removeItem(slot: Int, amount: Int): ItemStack {
            val current = getItem(slot)
            if (current.isEmpty) {
                return ItemStack.EMPTY
            }
            val removed = current.copyWithCount(minOf(amount, current.count))
            setItem(
                slot,
                when {
                    current.count <= amount -> ItemStack.EMPTY
                    else -> current.copyWithCount(current.count - amount)
                },
            )
            return removed
        }

        override fun removeItemNoUpdate(slot: Int): ItemStack {
            val current = getItem(slot)
            setItem(slot, ItemStack.EMPTY)
            return current
        }

        override fun setItem(slot: Int, stack: ItemStack) {
            when (slot) {
                in EQUIPMENT_SLOTS.indices -> target.setItemSlot(EQUIPMENT_SLOTS[slot], stack)
                EQUIPMENT_SLOTS.size -> target.setItemInHand(InteractionHand.OFF_HAND, stack)
                else -> Unit
            }
        }

        override fun setChanged() =
            target.inventory.setChanged()

        override fun stillValid(player: Player): Boolean =
            true

        override fun clearContent() {
            (0..EQUIPMENT_SLOTS.size).forEach {
                setItem(it, ItemStack.EMPTY)
            }
            setChanged()
        }

        private companion object {

            private val EQUIPMENT_SLOTS = listOf(
                EquipmentSlot.HEAD,
                EquipmentSlot.CHEST,
                EquipmentSlot.LEGS,
                EquipmentSlot.FEET,
            )

            private const val EQUIP_SLOTS: Int = 9

        }

    }

}

/** Read-only view over a snapshot container: reads are forwarded, every mutation is a no-op. */
private class ReadOnlyContainer(
    private val delegate: Container,
) : Container {

    override fun getContainerSize(): Int =
        delegate.containerSize

    override fun isEmpty(): Boolean =
        delegate.isEmpty

    override fun getItem(slot: Int): ItemStack =
        delegate.getItem(slot)

    override fun stillValid(player: Player): Boolean =
        delegate.stillValid(player)

    override fun setItem(slot: Int, stack: ItemStack) =
        Unit

    override fun removeItem(slot: Int, amount: Int): ItemStack =
        ItemStack.EMPTY

    override fun removeItemNoUpdate(slot: Int): ItemStack =
        ItemStack.EMPTY

    override fun setChanged() =
        Unit

    override fun clearContent() =
        Unit

    override fun canPlaceItem(slot: Int, stack: ItemStack): Boolean =
        false

    override fun canTakeItem(
        into: Container,
        slot: Int,
        stack: ItemStack
    ): Boolean =
        false

}
