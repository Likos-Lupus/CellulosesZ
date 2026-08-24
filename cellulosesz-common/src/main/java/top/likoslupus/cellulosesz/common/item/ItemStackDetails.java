package top.likoslupus.cellulosesz.common.item;

import static java.util.Objects.requireNonNull;

public record ItemStackDetails(
        String itemId,
        String displayName,
        int count,
        int maximumCount,
        boolean nonDefaultComponents,
        boolean damageable,
        int remainingDurability,
        int maximumDurability
) {

    public ItemStackDetails {
        itemId = requireNonNull(itemId, "itemId");
        displayName = requireNonNull(displayName, "displayName");
        if (count < 1 || maximumCount < 1) {
            throw new IllegalArgumentException("stack counts must be positive");
        }
        if (remainingDurability < 0 || maximumDurability < 0) {
            throw new IllegalArgumentException("durability must not be negative");
        }
    }

}
