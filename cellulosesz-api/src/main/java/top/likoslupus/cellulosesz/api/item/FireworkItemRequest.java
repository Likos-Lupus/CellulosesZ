package top.likoslupus.cellulosesz.api.item;

import java.util.List;
import java.util.Optional;

import static top.likoslupus.cellulosesz.api.validation.CollectionChecks.requireNonEmpty;
import static top.likoslupus.cellulosesz.api.validation.NumericChecks.requireNonNegative;
import static top.likoslupus.cellulosesz.api.validation.RangeChecks.requireInRange;

import static java.util.Objects.requireNonNull;

public record FireworkItemRequest(
        Operation operation,
        int power,
        Optional<FireworkShape> shape,
        List<Integer> colors,
        List<Integer> fadeColors,
        boolean trail,
        boolean flicker
) {

    public FireworkItemRequest {
        requireNonNull(operation, "operation");
        requireNonNull(shape, "shape");
        colors = List.copyOf(colors);
        fadeColors = List.copyOf(fadeColors);
        if (operation == Operation.POWER) {
            requireInRange(power, 1, 3, "power");
        } else {
            requireNonNegative(power, "power");
        }
        colors.forEach(color -> requireInRange(color, 0, 0xFFFFFF, "color"));
        fadeColors.forEach(color -> requireInRange(color, 0, 0xFFFFFF, "fadeColor"));
        if (operation == Operation.EFFECT) {
            if (shape.isEmpty()) {
                throw new IllegalArgumentException("shape is required for an effect request");
            }
            requireNonEmpty(colors, "colors");
        }
    }

    public static FireworkItemRequest clear() {
        return new FireworkItemRequest(
                Operation.CLEAR,
                0,
                Optional.empty(),
                List.of(),
                List.of(),
                false,
                false
        );
    }

    public static FireworkItemRequest power(int power) {
        return new FireworkItemRequest(
                Operation.POWER,
                power,
                Optional.empty(),
                List.of(),
                List.of(),
                false,
                false
        );
    }

    public static FireworkItemRequest effect(
            FireworkShape shape,
            List<Integer> colors,
            List<Integer> fadeColors,
            boolean trail,
            boolean flicker
    ) {
        return new FireworkItemRequest(
                Operation.EFFECT,
                0,
                Optional.of(shape),
                colors,
                fadeColors,
                trail,
                flicker
        );
    }

    public enum Operation {

        CLEAR,
        POWER,
        EFFECT

    }

}
