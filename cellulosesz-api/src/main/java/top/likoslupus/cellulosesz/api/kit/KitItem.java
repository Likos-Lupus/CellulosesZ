package top.likoslupus.cellulosesz.api.kit;

import static top.likoslupus.cellulosesz.api.validation.NumericChecks.requireNonNegative;
import static top.likoslupus.cellulosesz.api.validation.TextChecks.requireNonBlank;

import static java.util.Objects.requireNonNull;

/** Lossless inventory stack and its original player-inventory slot. */
public record KitItem(
        int slot,
        String stack
) {

    public KitItem {
        requireNonNegative(slot, "slot");
        stack = requireNonBlank(requireNonNull(stack, "stack").trim(), "stack");
    }

}
