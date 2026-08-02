package top.likoslupus.cellulosesz.modules.playerstate.command.argument;

import com.mojang.brigadier.LiteralMessage;
import com.mojang.brigadier.StringReader;
import com.mojang.brigadier.arguments.ArgumentType;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.mojang.brigadier.exceptions.DynamicCommandExceptionType;
import top.likoslupus.cellulosesz.api.playerstate.ExperienceUnit;

import java.util.Collection;
import java.util.List;

import static top.likoslupus.cellulosesz.api.validation.NumericChecks.requireNonNegative;

public final class ExperienceAmountArgument
        implements ArgumentType<ExperienceAmountArgument.Value> {

    private static final DynamicCommandExceptionType INVALID = new DynamicCommandExceptionType(
            value -> new LiteralMessage("Invalid experience amount: " + value)
    );

    private ExperienceAmountArgument() {
    }

    public static ExperienceAmountArgument amount() {
        return new ExperienceAmountArgument();
    }

    public static Value get(CommandContext<?> context, String name) {
        return context.getArgument(name, Value.class);
    }

    @Override
    public Value parse(StringReader reader) throws CommandSyntaxException {
        var start = reader.getCursor();
        var token = reader.readUnquotedString();

        try {
            var levels = token.endsWith("L") || token.endsWith("l");
            var number = levels
                    ? token.substring(0, token.length() - 1)
                    : token;

            if (number.isBlank()
                    || number.startsWith("+")
                    || number.startsWith("-")
            ) {
                throw new NumberFormatException();
            }

            return new Value(
                    Integer.parseInt(number),
                    levels
                            ? ExperienceUnit.LEVELS
                            : ExperienceUnit.POINTS
            );
        } catch (NumberFormatException _) {
            reader.setCursor(start);
            throw INVALID.createWithContext(reader, token);
        }
    }

    @Override
    public Collection<String> getExamples() {
        return List.of("100", "30L");
    }

    public record Value(
            int amount,
            ExperienceUnit unit
    ) {

        public Value {
            requireNonNegative(amount, "amount");
        }

    }

}
