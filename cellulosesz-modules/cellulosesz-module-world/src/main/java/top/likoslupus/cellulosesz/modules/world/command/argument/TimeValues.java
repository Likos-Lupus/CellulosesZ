package top.likoslupus.cellulosesz.modules.world.command.argument;

import com.mojang.brigadier.LiteralMessage;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.mojang.brigadier.exceptions.SimpleCommandExceptionType;

import java.util.List;
import java.util.Locale;

public final class TimeValues {

    private static final SimpleCommandExceptionType INVALID = new SimpleCommandExceptionType(
            new LiteralMessage(
                    "Time must be day, noon, night, midnight, or a non-negative tick value"
            )
    );
    private static final List<String> SUGGESTIONS = List.of("day", "noon", "night", "midnight");

    private TimeValues() {
    }

    public static long parse(String raw) throws CommandSyntaxException {
        var value = raw.toLowerCase(Locale.ROOT);
        var literal = switch (value) {
            case "day" -> 1_000L;
            case "noon" -> 6_000L;
            case "night" -> 13_000L;
            case "midnight" -> 18_000L;
            default -> -1L;
        };

        if (literal >= 0L) {
            return literal;
        }

        try {
            var ticks = Long.parseLong(value);
            if (ticks < 0L) {
                throw new NumberFormatException();
            }

            return ticks;
        } catch (NumberFormatException failure) {
            throw INVALID.create();
        }
    }

    public static List<String> suggestions() {
        return SUGGESTIONS;
    }

}
