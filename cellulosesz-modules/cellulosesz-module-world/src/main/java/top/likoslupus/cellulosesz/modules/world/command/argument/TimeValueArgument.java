package top.likoslupus.cellulosesz.modules.world.command.argument;

import com.mojang.brigadier.LiteralMessage;
import com.mojang.brigadier.StringReader;
import com.mojang.brigadier.arguments.ArgumentType;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.mojang.brigadier.exceptions.SimpleCommandExceptionType;

import java.util.Collection;
import java.util.List;
import java.util.Locale;

public final class TimeValueArgument implements ArgumentType<Long> {

    private static final SimpleCommandExceptionType INVALID = new SimpleCommandExceptionType(
            new LiteralMessage(
                    "Time must be day, noon, night, midnight, or a non-negative tick value"
            )
    );

    public static TimeValueArgument timeValue() {
        return new TimeValueArgument();
    }

    public static long get(CommandContext<?> context, String name) {
        return context.getArgument(name, Long.class);
    }

    @Override
    public Long parse(StringReader reader) throws CommandSyntaxException {
        var start = reader.getCursor();
        var value = reader.readUnquotedString().toLowerCase(Locale.ROOT);
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
            reader.setCursor(start);
            throw INVALID.createWithContext(reader);
        }
    }

    @Override
    public Collection<String> getExamples() {
        return List.of(
                "day",
                "18000",
                "0"
        );
    }

}
