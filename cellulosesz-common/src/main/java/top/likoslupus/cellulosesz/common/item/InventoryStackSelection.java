package top.likoslupus.cellulosesz.common.item;

import static java.util.Objects.requireNonNull;

public record InventoryStackSelection(
        InventoryItemSnapshot snapshot,
        int count
) {

    public InventoryStackSelection {
        requireNonNull(snapshot, "snapshot");
        if (count <= 0) {
            throw new IllegalArgumentException("count must be positive");
        }
        requireNonNull(snapshot, "snapshot");
    }

}
