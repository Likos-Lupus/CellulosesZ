package top.likoslupus.cellulosesz.api.event;

import top.likoslupus.cellulosesz.api.platform.CellPlayer;

import static java.util.Objects.requireNonNull;

public record InventoryCloseEvent(
        CellPlayer player,
        String inventoryType
) {

    public InventoryCloseEvent {
        requireNonNull(player, "player");
        requireNonNull(inventoryType, "inventoryType");
    }

}
