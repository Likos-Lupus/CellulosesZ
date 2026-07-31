package top.likoslupus.cellulosesz.api.validation;

import java.math.BigDecimal;
import java.time.Duration;
import java.util.Collection;
import java.util.Map;

import static java.util.Objects.requireNonNull;

/**
 * Stable checks for programmer-visible invariants. User input, configuration documents and persisted documents must
 * translate failures at their own boundaries instead of exposing these exceptions to players.
 */
@SuppressWarnings("UnusedReturnValue")
public final class Checks {

    private Checks() {
        throw new AssertionError("No instances");
    }

    public static String requireNonBlank(String value, String name) {
        value = requireArgumentNonNull(value, name);
        if (value.isBlank()) {
            throw new IllegalArgumentException(name + " must not be blank");
        }
        return value;
    }

    private static <T> T requireArgumentNonNull(T value, String name) {
        name = requireName(name);
        return requireNonNull(value, name + " must not be null");
    }

    private static String requireName(String name) {
        requireNonNull(name, "name must not be null");
        if (name.isBlank()) {
            throw new IllegalArgumentException("name must not be blank");
        }
        return name;
    }

    public static String requireNonEmpty(String value, String name) {
        value = requireArgumentNonNull(value, name);
        if (value.isEmpty()) {
            throw new IllegalArgumentException(name + " must not be empty");
        }
        return value;
    }

    public static <C extends Collection<?>> C requireNonEmpty(C collection, String name) {
        collection = requireArgumentNonNull(collection, name);
        if (collection.isEmpty()) {
            throw new IllegalArgumentException(name + " must not be empty");
        }
        return collection;
    }

    public static <M extends Map<?, ?>> M requireNonEmpty(M map, String name) {
        map = requireArgumentNonNull(map, name);
        if (map.isEmpty()) {
            throw new IllegalArgumentException(name + " must not be empty");
        }
        return map;
    }

    public static int requireGreaterThan(
            int value,
            int minimumExclusive,
            String name
    ) {
        name = requireName(name);
        if (value <= minimumExclusive) {
            throw new IllegalArgumentException(
                    name + " must be greater than " + minimumExclusive + ", but was " + value
            );
        }
        return value;
    }

    public static int requireGreaterThan(
            int value,
            String name,
            int other,
            String otherName
    ) {
        name = requireName(name);
        otherName = requireName(otherName);
        if (value <= other) {
            throw new IllegalArgumentException(
                    name + " must be greater than " + otherName + " (" + other + "), but was " + value
            );
        }
        return value;
    }

    public static long requireGreaterThan(
            long value,
            long minimumExclusive,
            String name
    ) {
        name = requireName(name);
        if (value <= minimumExclusive) {
            throw new IllegalArgumentException(
                    name + " must be greater than " + minimumExclusive + ", but was " + value
            );
        }
        return value;
    }

    public static double requireGreaterThan(
            double value,
            double minimumExclusive,
            String name
    ) {
        name = requireName(name);
        requireFinite(value, name);
        requireFinite(minimumExclusive, name + " minimumExclusive");
        if (value <= minimumExclusive) {
            throw new IllegalArgumentException(
                    name + " must be greater than " + minimumExclusive + ", but was " + value
            );
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

    public static BigDecimal requireGreaterThan(
            BigDecimal value,
            BigDecimal minimumExclusive,
            String name
    ) {
        value = requireArgumentNonNull(value, name);
        minimumExclusive = requireArgumentNonNull(minimumExclusive, "minimumExclusive");
        if (value.compareTo(minimumExclusive) <= 0) {
            throw new IllegalArgumentException(
                    name + " must be greater than " + minimumExclusive + ", but was " + value
            );
        }
        return value;
    }

    public static int requireAtLeast(
            int value,
            int minimumInclusive,
            String name
    ) {
        name = requireName(name);
        if (value < minimumInclusive) {
            throw new IllegalArgumentException(
                    name + " must be at least " + minimumInclusive + ", but was " + value
            );
        }
        return value;
    }

    public static long requireAtLeast(
            long value,
            long minimumInclusive,
            String name
    ) {
        name = requireName(name);
        if (value < minimumInclusive) {
            throw new IllegalArgumentException(
                    name + " must be at least " + minimumInclusive + ", but was " + value
            );
        }
        return value;
    }

    public static double requireAtLeast(
            double value,
            double minimumInclusive,
            String name
    ) {
        name = requireName(name);
        requireFinite(value, name);
        requireFinite(minimumInclusive, name + " minimumInclusive");
        if (value < minimumInclusive) {
            throw new IllegalArgumentException(
                    name + " must be at least " + minimumInclusive + ", but was " + value
            );
        }
        return value;
    }

    public static BigDecimal requireAtLeast(
            BigDecimal value,
            BigDecimal minimumInclusive,
            String name
    ) {
        value = requireArgumentNonNull(value, name);
        minimumInclusive = requireArgumentNonNull(minimumInclusive, "minimumInclusive");
        if (value.compareTo(minimumInclusive) < 0) {
            throw new IllegalArgumentException(
                    name + " must be at least " + minimumInclusive + ", but was " + value
            );
        }
        return value;
    }

    public static int requireLessThan(
            int value,
            int maximumExclusive,
            String name
    ) {
        name = requireName(name);
        if (value >= maximumExclusive) {
            throw new IllegalArgumentException(
                    name + " must be less than " + maximumExclusive + ", but was " + value
            );
        }
        return value;
    }

    public static long requireLessThan(
            long value,
            long maximumExclusive,
            String name
    ) {
        name = requireName(name);
        if (value >= maximumExclusive) {
            throw new IllegalArgumentException(
                    name + " must be less than " + maximumExclusive + ", but was " + value
            );
        }
        return value;
    }

    public static double requireLessThan(
            double value,
            double maximumExclusive,
            String name
    ) {
        name = requireName(name);
        requireFinite(value, name);
        requireFinite(maximumExclusive, name + " maximumExclusive");
        if (value >= maximumExclusive) {
            throw new IllegalArgumentException(
                    name + " must be less than " + maximumExclusive + ", but was " + value
            );
        }
        return value;
    }

    public static BigDecimal requireLessThan(
            BigDecimal value,
            BigDecimal maximumExclusive,
            String name
    ) {
        value = requireArgumentNonNull(value, name);
        maximumExclusive = requireArgumentNonNull(maximumExclusive, "maximumExclusive");
        if (value.compareTo(maximumExclusive) >= 0) {
            throw new IllegalArgumentException(
                    name + " must be less than " + maximumExclusive + ", but was " + value
            );
        }
        return value;
    }

    public static int requireAtMost(
            int value,
            int maximumInclusive,
            String name
    ) {
        name = requireName(name);
        if (value > maximumInclusive) {
            throw new IllegalArgumentException(
                    name + " must be at most " + maximumInclusive + ", but was " + value
            );
        }
        return value;
    }

    public static long requireAtMost(
            long value,
            long maximumInclusive,
            String name
    ) {
        name = requireName(name);
        if (value > maximumInclusive) {
            throw new IllegalArgumentException(
                    name + " must be at most " + maximumInclusive + ", but was " + value
            );
        }
        return value;
    }

    public static double requireAtMost(
            double value,
            double maximumInclusive,
            String name
    ) {
        name = requireName(name);
        requireFinite(value, name);
        requireFinite(maximumInclusive, name + " maximumInclusive");
        if (value > maximumInclusive) {
            throw new IllegalArgumentException(
                    name + " must be at most " + maximumInclusive + ", but was " + value
            );
        }
        return value;
    }

    public static BigDecimal requireAtMost(
            BigDecimal value,
            BigDecimal maximumInclusive,
            String name
    ) {
        value = requireArgumentNonNull(value, name);
        maximumInclusive = requireArgumentNonNull(maximumInclusive, "maximumInclusive");
        if (value.compareTo(maximumInclusive) > 0) {
            throw new IllegalArgumentException(
                    name + " must be at most " + maximumInclusive + ", but was " + value
            );
        }
        return value;
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

    public static BigDecimal requirePositive(BigDecimal value, String name) {
        value = requireArgumentNonNull(value, name);
        if (value.signum() <= 0) {
            throw new IllegalArgumentException(name + " must be greater than 0, but was " + value);
        }
        return value;
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
            throw new IllegalArgumentException(name + " must be positive or equal to -1, but was " + value);
        }
        return value;
    }

    public static long requirePositiveOrNegativeOne(long value, String name) {
        name = requireName(name);
        if (value < -1L || value == 0L) {
            throw new IllegalArgumentException(name + " must be positive or equal to -1, but was " + value);
        }
        return value;
    }

    public static int requireInRange(
            int value,
            int minimum,
            int maximum,
            String name
    ) {
        name = requireName(name);
        requireValidRange(minimum, maximum, name);
        if (value < minimum || value > maximum) {
            throw new IllegalArgumentException(
                    name + " must be in [" + minimum + ", " + maximum + "], but was " + value
            );
        }
        return value;
    }

    private static void requireValidRange(
            long minimum,
            long maximum,
            String name
    ) {
        if (minimum > maximum) {
            throw new IllegalArgumentException(requireName(name) + " minimum must not exceed maximum");
        }
    }

    public static long requireInRange(
            long value,
            long minimum,
            long maximum,
            String name
    ) {
        name = requireName(name);
        requireValidRange(minimum, maximum, name);
        if (value < minimum || value > maximum) {
            throw new IllegalArgumentException(
                    name + " must be in [" + minimum + ", " + maximum + "], but was " + value
            );
        }
        return value;
    }

    public static double requireInRange(
            double value,
            double minimum,
            double maximum,
            String name
    ) {
        name = requireName(name);
        requireFinite(value, name);
        requireFinite(minimum, name + " minimum");
        requireFinite(maximum, name + " maximum");
        if (minimum > maximum) {
            throw new IllegalArgumentException(name + " minimum must not exceed maximum");
        }
        if (value < minimum || value > maximum) {
            throw new IllegalArgumentException(
                    name + " must be in [" + minimum + ", " + maximum + "], but was " + value
            );
        }
        return value;
    }

    public static BigDecimal requireInRange(
            BigDecimal value,
            BigDecimal minimum,
            BigDecimal maximum,
            String name
    ) {
        value = requireArgumentNonNull(value, name);
        minimum = requireArgumentNonNull(minimum, "minimum");
        maximum = requireArgumentNonNull(maximum, "maximum");
        if (minimum.compareTo(maximum) > 0) {
            throw new IllegalArgumentException(name + " minimum must not exceed maximum");
        }
        if (value.compareTo(minimum) < 0 || value.compareTo(maximum) > 0) {
            throw new IllegalArgumentException(
                    name + " must be in [" + minimum + ", " + maximum + "], but was " + value
            );
        }
        return value;
    }

    public static String requireMinLength(
            String value,
            int minimum,
            String name
    ) {
        value = requireArgumentNonNull(value, name);
        requireNonNegative(minimum, "minimum");
        if (value.length() < minimum) {
            throw new IllegalArgumentException(
                    name + " length must be at least " + minimum + ", but was " + value.length()
            );
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

    public static String requireMaxLength(
            String value,
            int maximum,
            String name
    ) {
        value = requireArgumentNonNull(value, name);
        requireNonNegative(maximum, "maximum");
        if (value.length() > maximum) {
            throw new IllegalArgumentException(
                    name + " length must be at most " + maximum + ", but was " + value.length()
            );
        }
        return value;
    }

    public static String requireLengthInRange(
            String value,
            int minimum,
            int maximum,
            String name
    ) {
        value = requireArgumentNonNull(value, name);
        requireValidRange(minimum, maximum, name + " length");
        int length = value.length();
        if (length < minimum || length > maximum) {
            throw new IllegalArgumentException(
                    name + " length must be in [" + minimum + ", " + maximum + "], but was " + length
            );
        }
        return value;
    }

    public static void requireFalse(boolean condition, String message) {
        requireTrue(!condition, message);
    }

    public static void requireTrue(boolean condition, String message) {
        if (!condition) {
            throw new IllegalStateException(requireNonNull(message, "message must not be null"));
        }
    }

    public static String requireNoControlCharacters(String value, String name) {
        value = requireArgumentNonNull(value, name);
        for (int offset = 0; offset < value.length(); ) {
            int codePoint = value.codePointAt(offset);
            if (Character.isISOControl(codePoint)) {
                throw new IllegalArgumentException(
                        name + " must not contain control character U+%04X".formatted(codePoint)
                );
            }
            offset += Character.charCount(codePoint);
        }
        return value;
    }

    public static long requireGreaterThan(
            long value,
            String name,
            long other,
            String otherName
    ) {
        name = requireName(name);
        otherName = requireName(otherName);
        if (value <= other) {
            throw new IllegalArgumentException(
                    name + " must be greater than " + otherName + " (" + other + "), but was " + value
            );
        }
        return value;
    }

    public static double requireGreaterThan(
            double value,
            String name,
            double other,
            String otherName
    ) {
        name = requireName(name);
        otherName = requireName(otherName);
        requireFinite(value, name);
        requireFinite(other, otherName);
        if (value <= other) {
            throw new IllegalArgumentException(
                    name + " must be greater than " + otherName + " (" + other + "), but was " + value
            );
        }
        return value;
    }

    public static BigDecimal requireGreaterThan(
            BigDecimal value,
            String name,
            BigDecimal other,
            String otherName
    ) {
        value = requireArgumentNonNull(value, name);
        other = requireArgumentNonNull(other, otherName);
        if (value.compareTo(other) <= 0) {
            throw new IllegalArgumentException(
                    name + " must be greater than " + otherName + " (" + other + "), but was " + value
            );
        }
        return value;
    }

    public static int requireAtLeast(
            int value,
            String name,
            int other,
            String otherName
    ) {
        name = requireName(name);
        otherName = requireName(otherName);
        if (value < other) {
            throw new IllegalArgumentException(
                    name + " must be at least " + otherName + " (" + other + "), but was " + value
            );
        }
        return value;
    }

    public static long requireAtLeast(
            long value,
            String name,
            long other,
            String otherName
    ) {
        name = requireName(name);
        otherName = requireName(otherName);
        if (value < other) {
            throw new IllegalArgumentException(
                    name + " must be at least " + otherName + " (" + other + "), but was " + value
            );
        }
        return value;
    }

    public static double requireAtLeast(
            double value,
            String name,
            double other,
            String otherName
    ) {
        name = requireName(name);
        otherName = requireName(otherName);
        requireFinite(value, name);
        requireFinite(other, otherName);
        if (value < other) {
            throw new IllegalArgumentException(
                    name + " must be at least " + otherName + " (" + other + "), but was " + value
            );
        }
        return value;
    }

    public static BigDecimal requireAtLeast(
            BigDecimal value,
            String name,
            BigDecimal other,
            String otherName
    ) {
        value = requireArgumentNonNull(value, name);
        other = requireArgumentNonNull(other, otherName);
        if (value.compareTo(other) < 0) {
            throw new IllegalArgumentException(
                    name + " must be at least " + otherName + " (" + other + "), but was " + value
            );
        }
        return value;
    }

    public static int requireLessThan(
            int value,
            String name,
            int other,
            String otherName
    ) {
        name = requireName(name);
        otherName = requireName(otherName);
        if (value >= other) {
            throw new IllegalArgumentException(
                    name + " must be less than " + otherName + " (" + other + "), but was " + value
            );
        }
        return value;
    }

    public static long requireLessThan(
            long value,
            String name,
            long other,
            String otherName
    ) {
        name = requireName(name);
        otherName = requireName(otherName);
        if (value >= other) {
            throw new IllegalArgumentException(
                    name + " must be less than " + otherName + " (" + other + "), but was " + value
            );
        }
        return value;
    }

    public static double requireLessThan(
            double value,
            String name,
            double other,
            String otherName
    ) {
        name = requireName(name);
        otherName = requireName(otherName);
        requireFinite(value, name);
        requireFinite(other, otherName);
        if (value >= other) {
            throw new IllegalArgumentException(
                    name + " must be less than " + otherName + " (" + other + "), but was " + value
            );
        }
        return value;
    }

    public static BigDecimal requireLessThan(
            BigDecimal value,
            String name,
            BigDecimal other,
            String otherName
    ) {
        value = requireArgumentNonNull(value, name);
        other = requireArgumentNonNull(other, otherName);
        if (value.compareTo(other) >= 0) {
            throw new IllegalArgumentException(
                    name + " must be less than " + otherName + " (" + other + "), but was " + value
            );
        }
        return value;
    }

    public static int requireAtMost(
            int value,
            String name,
            int other,
            String otherName
    ) {
        name = requireName(name);
        otherName = requireName(otherName);
        if (value > other) {
            throw new IllegalArgumentException(
                    name + " must be at most " + otherName + " (" + other + "), but was " + value
            );
        }
        return value;
    }

    public static long requireAtMost(
            long value,
            String name,
            long other,
            String otherName
    ) {
        name = requireName(name);
        otherName = requireName(otherName);
        if (value > other) {
            throw new IllegalArgumentException(
                    name + " must be at most " + otherName + " (" + other + "), but was " + value
            );
        }
        return value;
    }

    public static double requireAtMost(
            double value,
            String name,
            double other,
            String otherName
    ) {
        name = requireName(name);
        otherName = requireName(otherName);
        requireFinite(value, name);
        requireFinite(other, otherName);
        if (value > other) {
            throw new IllegalArgumentException(
                    name + " must be at most " + otherName + " (" + other + "), but was " + value
            );
        }
        return value;
    }

    public static BigDecimal requireAtMost(
            BigDecimal value,
            String name,
            BigDecimal other,
            String otherName
    ) {
        value = requireArgumentNonNull(value, name);
        other = requireArgumentNonNull(other, otherName);
        if (value.compareTo(other) > 0) {
            throw new IllegalArgumentException(
                    name + " must be at most " + otherName + " (" + other + "), but was " + value
            );
        }
        return value;
    }

}
