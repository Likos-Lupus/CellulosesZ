package top.likoslupus.cellulosesz.api.item;

import static java.util.Objects.requireNonNull;

/**
 * Platform-neutral, lossless inventory stack snapshot.
 *
 * <p>{@code stack} is an opaque platform codec payload owned by the current CellulosesZ data model. API and module
 * code must not inspect or rewrite it; only the active platform adapter may encode or decode it.</p>
 */
public class InventoryItemSnapshot {

    public int slot;
    public String stack = "";

    public InventoryItemSnapshot() {
    }

    public InventoryItemSnapshot(
            int slot,
            String stack
    ) {
        if (slot < 0) throw new IllegalArgumentException("slot must not be negative");
        this.slot = slot;
        this.stack = requireStack(stack);
    }

    private static String requireStack(String value) {
        var normalized = requireNonNull(value, "stack").trim();
        if (normalized.isEmpty()) throw new IllegalArgumentException("stack must not be blank");
        return normalized;
    }

    public InventoryItemSnapshot copy() {
        return new InventoryItemSnapshot(slot, validatedStack());
    }

    public String validatedStack() {
        return requireStack(stack);
    }

}
