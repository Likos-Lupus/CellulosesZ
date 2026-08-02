package top.likoslupus.cellulosesz.modules.kit.persistence;

import top.likoslupus.cellulosesz.api.kit.KitDefinition;
import top.likoslupus.cellulosesz.api.kit.KitItem;

import java.math.BigDecimal;
import java.time.Duration;
import java.util.Optional;

import static java.util.Objects.requireNonNull;

/** Explicit conversion between kit YAML documents and immutable kit values. */
public final class KitMapper {

    private KitMapper() {
        throw new AssertionError("No instances");
    }

    public static KitDefinition toDomain(KitDocument document) {
        requireNonNull(document, "document");
        try {
            var items = requireNonNull(document.items, "items").stream()
                    .map(item -> new KitItem(
                            requireNonNull(item, "item").slot,
                            requireNonNull(item.stack, "item.stack")
                    ))
                    .toList();

            return new KitDefinition(
                    requireNonNull(document.id, "id"),
                    requireNonNull(document.displayName, "displayName"),
                    Optional.ofNullable(document.permission),
                    Duration.ofSeconds(document.cooldownSeconds),
                    new BigDecimal(requireNonNull(document.cost, "cost")),
                    items
            );
        } catch (RuntimeException failure) {
            throw new IllegalArgumentException(
                    "Invalid persisted kit document " + document.id,
                    failure
            );
        }
    }

    public static KitDocument fromDomain(KitDefinition kit) {
        requireNonNull(kit, "kit");
        var document = new KitDocument();
        document.id = kit.id();
        document.displayName = kit.displayName();
        document.permission = kit.permission().orElse("");
        document.cooldownSeconds = kit.cooldown().getSeconds();
        document.cost = kit.cost().toPlainString();
        document.items = kit.items().stream()
                .map(item -> {
                    var mapped = new KitItemDocument();
                    mapped.slot = item.slot();
                    mapped.stack = item.stack();
                    return mapped;
                })
                .toList();

        return document;
    }

}
