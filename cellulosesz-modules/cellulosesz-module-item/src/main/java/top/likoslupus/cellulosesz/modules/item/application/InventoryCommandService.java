package top.likoslupus.cellulosesz.modules.item.application;

import top.likoslupus.cellulosesz.api.platform.CellPlayer;
import top.likoslupus.cellulosesz.api.platform.operation.PlatformResult;
import top.likoslupus.cellulosesz.common.item.BookRequest;
import top.likoslupus.cellulosesz.common.item.HatAction;
import top.likoslupus.cellulosesz.common.item.InventoryPlatformService;
import top.likoslupus.cellulosesz.common.item.SkullRequest;

import java.util.concurrent.CompletableFuture;

import static java.util.Objects.requireNonNull;

/**
 * Inventory application operations shared by commands and signs.
 */
public final class InventoryCommandService {

    private final InventoryPlatformService inventory;

    public InventoryCommandService(InventoryPlatformService inventory) {
        this.inventory = requireNonNull(inventory, "inventory");
    }

    public InventoryPlatformService platform() {
        return inventory;
    }

    public PlatformResult<?> openInventory(CellPlayer viewer, CellPlayer target) {
        return inventory.openInventory(viewer, target);
    }

    public PlatformResult<?> openEnderChest(CellPlayer viewer, CellPlayer target) {
        return inventory.openEnderChest(viewer, target);
    }

    public PlatformResult<?> more(
            CellPlayer player,
            int count,
            int maximum
    ) {
        return inventory.setHeldCount(player, count, maximum);
    }

    public PlatformResult<?> hat(
            CellPlayer player,
            HatAction action,
            boolean ignoreBinding
    ) {
        return inventory.hat(player, action, ignoreBinding);
    }

    public PlatformResult<?> book(CellPlayer player, BookRequest request) {
        return inventory.mutateBook(player, request);
    }

    public CompletableFuture<? extends PlatformResult<?>> skull(SkullRequest request) {
        return inventory.skull(request);
    }

}
