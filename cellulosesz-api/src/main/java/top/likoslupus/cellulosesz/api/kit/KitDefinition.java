package top.likoslupus.cellulosesz.api.kit;

import java.math.BigDecimal;
import java.time.Duration;
import java.util.List;
import java.util.Optional;

import static top.likoslupus.cellulosesz.api.validation.TextChecks.requireNonBlank;

import static java.util.Objects.requireNonNull;

/** Immutable kit definition exposed by the public API. */
public record KitDefinition(
        String id,
        String displayName,
        Optional<String> permission,
        Duration cooldown,
        BigDecimal cost,
        List<KitItem> items
) {

    public KitDefinition {
        id = requireNonBlank(requireNonNull(id, "id").trim(), "id");
        displayName = requireNonBlank(
                requireNonNull(displayName, "displayName").trim(),
                "displayName"
        );
        permission = requireNonNull(permission, "permission")
                .map(String::trim)
                .filter(value -> !value.isBlank());
        requireNonNull(cooldown, "cooldown");
        if (cooldown.getNano() != 0
                || cooldown.isNegative() && !cooldown.equals(Duration.ofSeconds(-1))
        ) {
            throw new IllegalArgumentException(
                    "cooldown must be whole seconds and at least zero or exactly -1 second"
            );
        }
        requireNonNull(cost, "cost");
        if (cost.signum() < 0) {
            throw new IllegalArgumentException("cost must not be negative");
        }
        items = List.copyOf(requireNonNull(items, "items"));
        if (items.isEmpty()) {
            throw new IllegalArgumentException("items must not be empty");
        }
    }

}
