package top.likoslupus.cellulosesz.modules.admin.command.argument;

import com.mojang.brigadier.LiteralMessage;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.mojang.brigadier.exceptions.DynamicCommandExceptionType;

import java.time.Duration;
import java.util.Locale;

import static java.util.Objects.requireNonNull;

public final class AdminDurations {

    private static final DynamicCommandExceptionType INVALID = new DynamicCommandExceptionType(
            value -> new LiteralMessage("Invalid positive duration: " + value)
    );

    private AdminDurations() {
    }

    public static Duration parse(String raw, Duration maximum) throws CommandSyntaxException {
        requireNonNull(maximum, "maximum");
        if (maximum.isZero() || maximum.isNegative()) {
            throw new IllegalArgumentException("maximum must be positive");
        }

        var token = raw.trim().toLowerCase(Locale.ROOT);
        try {
            if (token.isEmpty()) {
                throw new IllegalArgumentException();
            }

            var split = token.length();
            while (split > 0 && Character.isLetter(token.charAt(split - 1))) {
                split--;
            }

            var amount = Long.parseLong(token.substring(0, split));
            if (amount <= 0) {
                throw new IllegalArgumentException();
            }

            var unit = token.substring(split);
            var duration = switch (unit) {
                case "", "s" -> Duration.ofSeconds(amount);
                case "ms" -> Duration.ofMillis(amount);
                case "m" -> Duration.ofMinutes(amount);
                case "h" -> Duration.ofHours(amount);
                case "d" -> Duration.ofDays(amount);
                case "w" -> Duration.ofDays(Math.multiplyExact(amount, 7L));
                default -> throw new IllegalArgumentException();
            };

            if (duration.compareTo(maximum) > 0) {
                throw new IllegalArgumentException();
            }
            return duration;
        } catch (ArithmeticException | IllegalArgumentException failure) {
            throw INVALID.create(raw);
        }
    }

}
