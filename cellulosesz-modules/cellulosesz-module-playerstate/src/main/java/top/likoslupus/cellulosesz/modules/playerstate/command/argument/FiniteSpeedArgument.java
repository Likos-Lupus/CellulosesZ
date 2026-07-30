package top.likoslupus.cellulosesz.modules.playerstate.command.argument;

import com.mojang.brigadier.LiteralMessage;
import com.mojang.brigadier.StringReader;
import com.mojang.brigadier.arguments.ArgumentType;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.mojang.brigadier.exceptions.DynamicCommandExceptionType;

import java.util.Collection;
import java.util.List;

public final class FiniteSpeedArgument implements ArgumentType<Double> {

    private static final DynamicCommandExceptionType INVALID = new DynamicCommandExceptionType(
            value -> new LiteralMessage("Invalid movement speed: " + value)
    );

    private final double minimum;
    private final double maximum;

    private FiniteSpeedArgument(
            double minimum,
            double maximum
    ) {
        this.minimum = minimum;
        this.maximum = maximum;
    }

    public static FiniteSpeedArgument speed(double minimum, double maximum) {
        return new FiniteSpeedArgument(minimum, maximum);
    }

    public static double get(CommandContext<?> context, String name) {
        return context.getArgument(name, Double.class);
    }

    @Override
    public Double parse(StringReader reader) throws CommandSyntaxException {
        var start = reader.getCursor();
        var token = reader.readUnquotedString();

        try {
            var value = Double.parseDouble(token);
            if (!Double.isFinite(value)
                    || value < minimum
                    || value > maximum
            ) {
                throw new NumberFormatException();
            }

            return value;
        } catch (NumberFormatException _) {
            reader.setCursor(start);
            throw INVALID.createWithContext(reader, token);
        }
    }

    @Override
    public Collection<String> getExamples() {
        return List.of("1", "2.5", "10");
    }

}
