package top.likoslupus.cellulosesz.api.item;

import java.util.Locale;

import static top.likoslupus.cellulosesz.api.validation.Checks.requireNonBlank;
import static top.likoslupus.cellulosesz.api.validation.Checks.requirePositive;

import static java.util.Objects.requireNonNull;

/**
 * Registry-validated item input together with the requested business count.
 *
 * <p>{@link #argument} is opaque vanilla item-command syntax. CellulosesZ does
 * not interpret or serialize its data-component grammar.</p>
 */
public class ItemDescriptor {

    public String item = "minecraft:air";
    public int count = 1;
    public String argument = "";

    public ItemDescriptor() {
    }

    public ItemDescriptor(
            String item,
            int count
    ) {
        this(item, count, item);
    }

    public ItemDescriptor(
            String item,
            int count,
            String argument
    ) {
        this.item = requireNonNull(item, "item");
        this.count = requirePositive(count, "count");
        this.argument = requireNonNull(argument, "argument");
    }

    public ItemDescriptor copy() {
        return new ItemDescriptor(normalizedItem(), count, normalizedArgument());
    }

    public String normalizedItem() {
        var value = requireNonBlank(
                requireNonNull(item, "item").trim().toLowerCase(Locale.ROOT),
                "item"
        );
        return value.indexOf(':') < 0
                ? "minecraft:" + value
                : value;
    }

    public String normalizedArgument() {
        var value = requireNonNull(argument, "argument").trim();
        return value.isBlank()
                ? normalizedItem()
                : value;
    }

}
