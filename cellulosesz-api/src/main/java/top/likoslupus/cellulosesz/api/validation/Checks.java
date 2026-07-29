package top.likoslupus.cellulosesz.api.validation;

import java.math.BigDecimal;

import static java.util.Objects.requireNonNull;

/**
 * Stable checks for programmer-visible invariants. User input, configuration documents and persisted documents must
 * translate failures at their own boundaries instead of exposing these exceptions to players.
 */
public final class Checks {

    private Checks() {
    }

    public static String requireNonBlank(String value, String name) {
        requireNonNull(value, name);
        if (value.isBlank()) {
            throw new IllegalArgumentException(name + " must not be blank");
        }
        return value;
    }

    public static int requirePositive(int value, String name) {
        if (value <= 0) {
            throw new IllegalArgumentException(name + " must be greater than 0, but was " + value);
        }
        return value;
    }

    public static long requirePositive(long value, String name) {
        if (value <= 0L) {
            throw new IllegalArgumentException(name + " must be greater than 0, but was " + value);
        }
        return value;
    }

    public static BigDecimal requirePositive(BigDecimal value, String name) {
        requireNonNull(value, name);
        if (value.signum() <= 0) {
            throw new IllegalArgumentException(name + " must be greater than 0, but was " + value);
        }
        return value;
    }

    public static long requireNonNegative(long value, String name) {
        if (value < 0L) {
            throw new IllegalArgumentException(name + " must be at least 0, but was " + value);
        }
        return value;
    }

    public static BigDecimal requireNonNegative(BigDecimal value, String name) {
        requireNonNull(value, name);
        if (value.signum() < 0) {
            throw new IllegalArgumentException(name + " must be at least 0, but was " + value);
        }
        return value;
    }

    public static String requireMaxLength(
            String value,
            int maximum,
            String name
    ) {
        requireNonNull(value, name);
        requireNonNegative(maximum, "maximum");
        if (value.length() > maximum) {
            throw new IllegalArgumentException(name + " length must be at most " + maximum + ", but was " + value.length());
        }
        return value;
    }

    public static int requireNonNegative(int value, String name) {
        if (value < 0) {
            throw new IllegalArgumentException(name + " must be at least 0, but was " + value);
        }
        return value;
    }

    public static double requireNonNegative(double value, String name) {
        requireFinite(value, name);
        if (value < 0.0D) throw new IllegalArgumentException(name + " must be at least 0, but was " + value);
        return value;
    }

    public static double requireFinite(double value, String name) {
        if (!Double.isFinite(value)) {
            throw new IllegalArgumentException(name + " must be finite, but was " + value);
        }
        return value;
    }

    public static double requirePositive(double value, String name) {
        requireFinite(value, name);
        if (value <= 0.0D) throw new IllegalArgumentException(name + " must be greater than 0, but was " + value);
        return value;
    }

    public static int requireRange(int value, int minimum, int maximum, String name) {
        return requireInRange(value, minimum, maximum, name);
    }

    public static int requireInRange(
            int value,
            int minimum,
            int maximum,
            String name
    ) {
        requireValidRange(minimum, maximum, name);
        if (value < minimum || value > maximum) {
            throw new IllegalArgumentException(name + " must be in [" + minimum + ", " + maximum + "], but was " + value);
        }
        return value;
    }

    private static void requireValidRange(
            long minimum,
            long maximum,
            String name
    ) {
        if (minimum > maximum) {
            throw new IllegalArgumentException(name + " minimum must not exceed maximum");
        }
    }

    public static long requireRange(
            long value,
            long minimum,
            long maximum,
            String name
    ) {
        return requireInRange(value, minimum, maximum, name);
    }

    public static long requireInRange(
            long value,
            long minimum,
            long maximum,
            String name
    ) {
        requireValidRange(minimum, maximum, name);
        if (value < minimum || value > maximum) {
            throw new IllegalArgumentException(name + " must be in [" + minimum + ", " + maximum + "], but was " + value);
        }
        return value;
    }

    public static double requireRange(
            double value,
            double minimum,
            double maximum,
            String name
    ) {
        return requireInRange(value, minimum, maximum, name);
    }

    public static double requireInRange(
            double value,
            double minimum,
            double maximum,
            String name
    ) {
        requireFinite(value, name);
        requireFinite(minimum, name + " minimum");
        requireFinite(maximum, name + " maximum");
        if (minimum > maximum) {
            throw new IllegalArgumentException(name + " minimum must not exceed maximum");
        }
        if (value < minimum || value > maximum) {
            throw new IllegalArgumentException(name + " must be in [" + minimum + ", " + maximum + "], but was " + value);
        }
        return value;
    }

    public static void requireState(boolean condition, String message) {
        if (!condition) throw new IllegalStateException(requireNonNull(message, "message"));
    }

    public static String requireNoControlCharacters(String value, String name) {
        requireNonNull(value, name);
        for (int offset = 0; offset < value.length(); ) {
            var codePoint = value.codePointAt(offset);
            if (Character.isISOControl(codePoint)) {
                throw new IllegalArgumentException(name + " must not contain control character U+%04X".formatted(codePoint));
            }
            offset += Character.charCount(codePoint);
        }
        return value;
    }

}
