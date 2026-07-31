package top.likoslupus.cellulosesz.modules.admin.command.argument;

import com.mojang.brigadier.LiteralMessage;
import com.mojang.brigadier.StringReader;
import com.mojang.brigadier.arguments.ArgumentType;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.mojang.brigadier.exceptions.DynamicCommandExceptionType;

import java.time.Duration;
import java.util.Collection;
import java.util.List;
import java.util.Locale;

import static java.util.Objects.requireNonNull;

public final class DurationArgument implements ArgumentType<Duration> {

    private static final DynamicCommandExceptionType INVALID = new DynamicCommandExceptionType(
            value -> new LiteralMessage("Invalid positive duration: " + value)
    );
    private final Duration maximum;

    private DurationArgument(Duration maximum) {
        this.maximum = requireNonNull(maximum, "maximum");
        if (maximum.isZero() || maximum.isNegative()) {
            throw new IllegalArgumentException("maximum must be positive");
        }
    }

    public static DurationArgument duration(Duration maximum) {
        return new DurationArgument(maximum);
    }

    public static Duration get(CommandContext<?> context, String name) {
        return context.getArgument(name, Duration.class);
    }

    @Override
    public Duration parse(StringReader reader) throws CommandSyntaxException {
        var start = reader.getCursor();
        var token = reader.readUnquotedString().trim().toLowerCase(Locale.ROOT);

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
            reader.setCursor(start);
            throw INVALID.createWithContext(reader, token);
        }
    }

    @Override
    public Collection<String> getExamples() {
        return List.of(
                "30",
                "500ms",
                "10m",
                "2h",
                "7d",
                "2w"
        );
    }

}
