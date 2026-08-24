package top.likoslupus.cellulosesz.common.item;

import top.likoslupus.cellulosesz.api.item.InventoryMutation;
import top.likoslupus.cellulosesz.api.item.ItemDescriptor;
import top.likoslupus.cellulosesz.api.platform.CellPlayer;
import top.likoslupus.cellulosesz.api.platform.operation.PlatformResult;

import java.util.List;
import java.util.concurrent.CompletableFuture;

public interface InventoryPlatformService {

    PlatformResult<Void> openInventory(CellPlayer viewer, CellPlayer target);

    PlatformResult<Void> openEnderChest(CellPlayer viewer, CellPlayer target);

    PlatformResult<List<InventoryItemSnapshot>> inventorySnapshot(CellPlayer player);

    PlatformResult<Boolean> plainSnapshot(InventoryItemSnapshot snapshot);

    PlatformResult<InventoryMutation> prepareExchange(
            CellPlayer player,
            List<InventoryItemRequest> removals,
            List<InventoryItemRequest> additions
    );

    PlatformResult<InventoryMutation> prepareGrant(
            CellPlayer player,
            List<InventoryItemSnapshot> snapshots
    );

    PlatformResult<List<InventorySlotView>> inventorySlots(CellPlayer player);

    /**
     * Returns the lossless snapshot of the current main-hand stack.
     */
    PlatformResult<InventorySlotView> heldSlot(CellPlayer player);

    /**
     * Prepares an exact-slot, conflict-detecting removal transaction.
     */
    PlatformResult<InventoryMutation> prepareRemoval(
            CellPlayer player,
            List<InventoryStackSelection> selections
    );

    PlatformResult<ItemDescriptor> describeSnapshot(InventoryItemSnapshot snapshot);

    PlatformResult<HeldStackChange> setHeldCount(
            CellPlayer player,
            int targetCount,
            int permittedMaximum
    );

    PlatformResult<HatResult> hat(
            CellPlayer player,
            HatAction action,
            boolean ignoreBindingCurse
    );

    PlatformResult<ItemStackDetails> heldItemDetails(CellPlayer player);

    PlatformResult<BookDetails> heldBook(CellPlayer player);

    PlatformResult<BookMutationResult> mutateBook(CellPlayer player, BookRequest request);

    CompletableFuture<PlatformResult<SkullResult>> skull(SkullRequest request);

}
