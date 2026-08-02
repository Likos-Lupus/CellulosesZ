package top.likoslupus.cellulosesz.api.validation;

import java.math.BigDecimal;
import java.time.Duration;

import static java.util.Objects.requireNonNull;

/** Focused validation checks. */
@SuppressWarnings("UnusedReturnValue")
public final class NumericChecks {

    private NumericChecks() {
        throw new AssertionError("No instances");
    }

    public static float requireFinite(float value, String name) {
        name = requireName(name);
        if (!Float.isFinite(value)) {
            throw new IllegalArgumentException(name + " must be finite, but was " + value);
        }
        return value;
    }

    private static String requireName(String name) {
        requireNonNull(name, "name must not be null");
        if (name.isBlank()) {
            throw new IllegalArgumentException("name must not be blank");
        }
        return name;
    }

    public static int requirePositive(int value, String name) {
        name = requireName(name);
        if (value <= 0) {
            throw new IllegalArgumentException(name + " must be greater than 0, but was " + value);
        }
        return value;
    }

    public static long requirePositive(long value, String name) {
        name = requireName(name);
        if (value <= 0L) {
            throw new IllegalArgumentException(name + " must be greater than 0, but was " + value);
        }
        return value;
    }

    public static double requirePositive(double value, String name) {
        name = requireName(name);
        requireFinite(value, name);
        if (value <= 0.0D) {
            throw new IllegalArgumentException(name + " must be greater than 0, but was " + value);
        }
        return value;
    }

    public static double requireFinite(double value, String name) {
        name = requireName(name);
        if (!Double.isFinite(value)) {
            throw new IllegalArgumentException(name + " must be finite, but was " + value);
        }
        return value;
    }

    public static BigDecimal requirePositive(BigDecimal value, String name) {
        value = requireArgumentNonNull(value, name);
        if (value.signum() <= 0) {
            throw new IllegalArgumentException(name + " must be greater than 0, but was " + value);
        }
        return value;
    }

    private static <T> T requireArgumentNonNull(T value, String name) {
        name = requireName(name);
        return requireNonNull(value, name + " must not be null");
    }

    public static Duration requirePositive(Duration value, String name) {
        value = requireArgumentNonNull(value, name);
        if (value.isNegative() || value.isZero()) {
            throw new IllegalArgumentException(name + " must be greater than 0, but was " + value);
        }
        return value;
    }

    public static long requireNonNegative(long value, String name) {
        name = requireName(name);
        if (value < 0L) {
            throw new IllegalArgumentException(name + " must be at least 0, but was " + value);
        }
        return value;
    }

    public static double requireNonNegative(double value, String name) {
        name = requireName(name);
        requireFinite(value, name);
        if (value < 0.0D) {
            throw new IllegalArgumentException(name + " must be at least 0, but was " + value);
        }
        return value;
    }

    public static BigDecimal requireNonNegative(BigDecimal value, String name) {
        value = requireArgumentNonNull(value, name);
        if (value.signum() < 0) {
            throw new IllegalArgumentException(name + " must be at least 0, but was " + value);
        }
        return value;
    }

    public static Duration requireNonNegative(Duration value, String name) {
        value = requireArgumentNonNull(value, name);
        if (value.isNegative()) {
            throw new IllegalArgumentException(name + " must be at least 0, but was " + value);
        }
        return value;
    }

    public static int requirePositiveOrNegativeOne(int value, String name) {
        name = requireName(name);
        if (value < -1 || value == 0) {
            throw new IllegalArgumentException(
                    name + " must be positive or equal to -1, but was " + value);
        }
        return value;
    }

    public static long requirePositiveOrNegativeOne(long value, String name) {
        name = requireName(name);
        if (value < -1L || value == 0L) {
            throw new IllegalArgumentException(
                    name + " must be positive or equal to -1, but was " + value);
        }
        return value;
    }

    public static int requireNonNegative(int value, String name) {
        name = requireName(name);
        if (value < 0) {
            throw new IllegalArgumentException(name + " must be at least 0, but was " + value);
        }
        return value;
    }

}
