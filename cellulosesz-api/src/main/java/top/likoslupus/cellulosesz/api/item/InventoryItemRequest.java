package top.likoslupus.cellulosesz.api.item;

import static java.util.Objects.requireNonNull;

/**
 * A validated item command argument and positive quantity used to prepare an inventory transaction.
 */
public record InventoryItemRequest(
        String itemArgument,
        int count
) {

    public InventoryItemRequest {
        itemArgument = requireNonNull(itemArgument, "itemArgument").trim();
        if (itemArgument.isEmpty()) throw new IllegalArgumentException("itemArgument must not be blank");
        if (count <= 0) throw new IllegalArgumentException("count must be positive");
    }

}
