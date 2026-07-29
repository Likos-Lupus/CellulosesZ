package top.likoslupus.cellulosesz.api.item;

import top.likoslupus.cellulosesz.api.platform.CellPlayer;
import top.likoslupus.cellulosesz.api.platform.operation.PlatformResult;

import java.util.List;
import java.util.concurrent.CompletableFuture;

public interface InventoryPlatformService {

    PlatformResult<List<InventorySlotView>> inventorySlots(CellPlayer player);

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
