package top.likoslupus.cellulosesz.api.item;

import top.likoslupus.cellulosesz.api.validation.Checks;

import java.util.Optional;

import static java.util.Objects.requireNonNull;

public record InventoryClearFilter(
        Kind kind,
        Optional<String> itemId
) {

    public InventoryClearFilter {
        requireNonNull(kind, "kind");
        itemId = requireNonNull(itemId, "itemId")
                .map(value -> Checks.requireNonBlank(value, "itemId").trim());
        if (kind == Kind.ITEM && itemId.isEmpty()) {
            throw new IllegalArgumentException("itemId is required for ITEM filter");
        }
        if (kind != Kind.ITEM && itemId.isPresent()) {
            throw new IllegalArgumentException("itemId is only valid for ITEM filter");
        }
    }

    public static InventoryClearFilter inventory() {
        return new InventoryClearFilter(Kind.ALL_INVENTORY, Optional.empty());
    }

    public static InventoryClearFilter withEquipment() {
        return new InventoryClearFilter(Kind.ALL_WITH_EQUIPMENT, Optional.empty());
    }

    public static InventoryClearFilter item(String itemId) {
        return new InventoryClearFilter(Kind.ITEM, Optional.of(itemId));
    }

    public enum Kind {

        ALL_INVENTORY,
        ALL_WITH_EQUIPMENT,
        ITEM

    }

}
