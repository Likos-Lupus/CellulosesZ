package top.likoslupus.cellulosesz.modules.admin.service;

import java.util.Locale;
import java.util.OptionalLong;

public final class DurationParser {

    private DurationParser() {
    }

    public static OptionalLong parseMillis(String input) {
        if (input.isBlank()) return OptionalLong.empty();
        var value = input.trim().toLowerCase(Locale.ROOT);

        return switch (value) {
            case String s when s.endsWith("ms") -> toMillis(s.substring(0, s.length() - 2), 1L);
            case String s when s.endsWith("s") -> toMillis(s.substring(0, s.length() - 1), 1000L);
            case String s when s.endsWith("m") -> toMillis(s.substring(0, s.length() - 1), 60_000L);
            case String s when s.endsWith("h") -> toMillis(s.substring(0, s.length() - 1), 3_600_000L);
            case String s when s.endsWith("d") -> toMillis(s.substring(0, s.length() - 1), 86_400_000L);
            case String s when s.endsWith("w") -> toMillis(s.substring(0, s.length() - 1), 604_800_000L);
            default -> toMillis(value, 1000L);
        };
    }

    private static OptionalLong toMillis(String numeric, long multiplier) {
        try {
            var amount = Long.parseLong(numeric);
            return amount <= 0L
                    ? OptionalLong.empty()
                    : OptionalLong.of(Math.multiplyExact(amount, multiplier));
        } catch (RuntimeException _) {
            return OptionalLong.empty();
        }
    }

}
