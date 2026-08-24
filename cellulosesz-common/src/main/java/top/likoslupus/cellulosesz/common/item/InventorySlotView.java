package top.likoslupus.cellulosesz.common.item;

import top.likoslupus.cellulosesz.api.item.ItemDescriptor;

import static java.util.Objects.requireNonNull;

public record InventorySlotView(
        InventoryItemSnapshot snapshot,
        ItemDescriptor descriptor,
        InventorySlotKind kind,
        boolean plain
) {

    public InventorySlotView {
        requireNonNull(snapshot, "snapshot");
        requireNonNull(descriptor, "descriptor");
        requireNonNull(kind, "kind");
    }

}
