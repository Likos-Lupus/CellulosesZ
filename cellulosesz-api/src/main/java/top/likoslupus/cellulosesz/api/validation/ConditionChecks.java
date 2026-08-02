package top.likoslupus.cellulosesz.api.validation;

import static java.util.Objects.requireNonNull;

/** Focused validation checks. */
@SuppressWarnings("UnusedReturnValue")
public final class ConditionChecks {

    private ConditionChecks() {
        throw new AssertionError("No instances");
    }

    public static void requireFalse(boolean condition, String message) {
        if (condition) {
            throw new IllegalStateException(requireNonNull(message, "message must not be null"));
        }
    }

    public static void requireTrue(boolean condition, String message) {
        if (!condition) {
            throw new IllegalStateException(requireNonNull(message, "message must not be null"));
        }
    }

}
