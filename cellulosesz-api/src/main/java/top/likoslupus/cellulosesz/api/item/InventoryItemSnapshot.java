package top.likoslupus.cellulosesz.api.item;

import static top.likoslupus.cellulosesz.api.validation.NumericChecks.requireNonNegative;
import static top.likoslupus.cellulosesz.api.validation.TextChecks.requireNonBlank;

import static java.util.Objects.requireNonNull;

/** Platform-neutral, lossless inventory stack snapshot. */
public record InventoryItemSnapshot(
        int slot,
        String stack
) {

    public InventoryItemSnapshot {
        requireNonNegative(slot, "slot");
        stack = requireNonBlank(requireNonNull(stack, "stack").trim(), "stack");
    }

}
