package top.likoslupus.cellulosesz.api.item;

import static java.util.Objects.requireNonNull;

public record HeldStackChange(
        String itemId,
        int previousCount,
        int currentCount,
        int normalMaximum
) {

    public HeldStackChange {
        itemId = requireNonNull(itemId, "itemId");
        if (previousCount < 1 || currentCount < 1 || normalMaximum < 1) {
            throw new IllegalArgumentException("stack counts must be positive");
        }
    }

}
