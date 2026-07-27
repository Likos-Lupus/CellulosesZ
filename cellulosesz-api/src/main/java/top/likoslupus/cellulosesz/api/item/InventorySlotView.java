package top.likoslupus.cellulosesz.api.item;

import static java.util.Objects.requireNonNull;

public record InventorySlotView(
        InventoryItemSnapshot snapshot,
        ItemDescriptor descriptor,
        InventorySlotKind kind,
        boolean plain
) {

    public InventorySlotView {
        snapshot = requireNonNull(snapshot, "snapshot").copy();
        descriptor = requireNonNull(descriptor, "descriptor").copy();
        requireNonNull(kind, "kind");
    }

}
