package top.likoslupus.cellulosesz.modules.messaging.service;

import java.time.Duration;
import java.util.Locale;
import java.util.OptionalLong;

public final class MailDurationParser {

    private MailDurationParser() {
    }

    public static OptionalLong parseMillis(String input) {
        if (input.isBlank()) return OptionalLong.empty();

        var value = input.trim().toLowerCase(Locale.ROOT);

        try {
            var duration = switch (value.charAt(value.length() - 1)) {
                case 's' -> Duration.ofSeconds(parseNumber(value));
                case 'm' -> Duration.ofMinutes(parseNumber(value));
                case 'h' -> Duration.ofHours(parseNumber(value));
                case 'd' -> Duration.ofDays(parseNumber(value));
                case 'w' -> Duration.ofDays(Math.multiplyExact(parseNumber(value), 7));
                default -> Duration.ofSeconds(Long.parseLong(value));
            };

            return duration.isZero() || duration.isNegative()
                    ? OptionalLong.empty()
                    : OptionalLong.of(duration.toMillis());
        } catch (RuntimeException e) {
            return OptionalLong.empty();
        }
    }

    private static long parseNumber(String value) {
        return Long.parseLong(value.substring(0, value.length() - 1));
    }

}
