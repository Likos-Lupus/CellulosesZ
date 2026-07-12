package top.likoslupus.cellulosesz.api.event;

import top.likoslupus.cellulosesz.api.platform.CellPlayer;

public record InventoryCloseEvent(
        CellPlayer player,
        String inventoryType
) {

}
