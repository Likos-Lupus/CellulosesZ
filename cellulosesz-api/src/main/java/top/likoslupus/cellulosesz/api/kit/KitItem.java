package top.likoslupus.cellulosesz.api.kit;

import top.likoslupus.cellulosesz.api.item.InventoryItemSnapshot;

/**
 * Lossless inventory stack and its original player-inventory slot.
 */
public final class KitItem extends InventoryItemSnapshot {

    public KitItem() {
    }

    public KitItem(
            int slot,
            String stack
    ) {
        super(slot, stack);
    }

}
