package top.likoslupus.cellulosesz.api.validation;

import static top.likoslupus.cellulosesz.api.validation.NumericChecks.requireNonNegative;

import static java.util.Objects.requireNonNull;

/** Focused validation checks. */
@SuppressWarnings("UnusedReturnValue")
public final class TextChecks {

    private TextChecks() {
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
                    name + " length must be in [" + minimum + ", " + maximum + "], but was "
                            + length
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
            throw new IllegalArgumentException(
                    requireName(name) + " minimum must not exceed maximum");
        }
    }

}
