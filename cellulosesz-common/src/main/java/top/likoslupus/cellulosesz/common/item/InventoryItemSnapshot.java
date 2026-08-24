package top.likoslupus.cellulosesz.common.item;

import static top.likoslupus.cellulosesz.api.validation.Checks.requireNonBlank;
import static top.likoslupus.cellulosesz.api.validation.Checks.requireNonNegative;

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
