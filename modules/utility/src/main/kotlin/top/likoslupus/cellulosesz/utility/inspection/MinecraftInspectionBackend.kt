package top.likoslupus.cellulosesz.utility.inspection

import net.minecraft.network.chat.Component.literal
import net.minecraft.network.chat.Component.translatable
import net.minecraft.server.level.ServerPlayer
import net.minecraft.world.Container
import net.minecraft.world.SimpleContainer
import net.minecraft.world.SimpleMenuProvider
import net.minecraft.world.entity.player.Inventory
import net.minecraft.world.entity.player.Player
import net.minecraft.world.inventory.ChestMenu
import net.minecraft.world.inventory.MenuType
import net.minecraft.world.item.ItemStack
import top.likoslupus.cellulosesz.core.player.PlayerResolver
import top.likoslupus.cellulosesz.core.runtime.RuntimeKernel
import java.util.*

/**
 * Opens read-only or throwaway vanilla chest menus. `/invsee` snapshots the target inventory into a
 * container whose mutation methods are disabled, so a viewer can never extract or alter items.
 */
internal class MinecraftInspectionBackend(
    private val kernel: RuntimeKernel,
) : InspectionBackend {

    override fun openEnderChest(playerId: UUID): InspectionOpenResult {
        val player = player(playerId)
            ?: return InspectionOpenResult.PlayerOffline
        val provider = SimpleMenuProvider(
            { id, inventory, _ ->
                ChestMenu.threeRows(
                    id,
                    inventory,
                    player.enderChestInventory
                )
            },
            translatable("container.enderchest"),
        )
        return open(player, provider)
    }

    override fun openInventoryView(viewerId: UUID, targetId: UUID): InspectionOpenResult {
        val viewer = player(viewerId)
            ?: return InspectionOpenResult.PlayerOffline
        val target = player(targetId)
            ?: return InspectionOpenResult.PlayerOffline

        val snapshot = SimpleContainer(INSPECT_ROWS * 9)
        val size = minOf(target.inventory.containerSize, snapshot.containerSize)
        (0 until size).forEach {
            snapshot.setItem(
                it,
                target.inventory.getItem(it).copy()
            )
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
