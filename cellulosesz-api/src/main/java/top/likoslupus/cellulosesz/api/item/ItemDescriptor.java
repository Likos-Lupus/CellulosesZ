package top.likoslupus.cellulosesz.api.item;

import java.util.Locale;

import static top.likoslupus.cellulosesz.api.validation.NumericChecks.requirePositive;
import static top.likoslupus.cellulosesz.api.validation.TextChecks.requireNonBlank;

import static java.util.Objects.requireNonNull;

/** Registry-validated item input together with the requested business count. */
public record ItemDescriptor(
        String item,
        int count,
        String argument
) {

    public ItemDescriptor(String item, int count) {
        this(item, count, item);
    }

    public ItemDescriptor {
        item = normalizeItem(item);
        requirePositive(count, "count");
        var normalizedArgument = requireNonNull(argument, "argument").trim();
        argument = normalizedArgument.isBlank()
                ? item
                : normalizedArgument;
    }

    private static String normalizeItem(String value) {
        var normalized = requireNonBlank(
                requireNonNull(value, "item")
                        .trim()
                        .toLowerCase(Locale.ROOT), "item"
        );
        return normalized.indexOf(':') < 0
                ? "minecraft:" + normalized
                : normalized;
    }

    public String normalizedItem() {
        return item;
    }

    public String normalizedArgument() {
        return argument;
    }

}
