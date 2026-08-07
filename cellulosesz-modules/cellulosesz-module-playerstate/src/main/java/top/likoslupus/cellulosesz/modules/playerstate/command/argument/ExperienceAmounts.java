package top.likoslupus.cellulosesz.modules.playerstate.command.argument;

import com.mojang.brigadier.LiteralMessage;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.mojang.brigadier.exceptions.DynamicCommandExceptionType;
import top.likoslupus.cellulosesz.api.playerstate.ExperienceUnit;

import static top.likoslupus.cellulosesz.api.validation.NumericChecks.requireNonNegative;

public final class ExperienceAmounts {

    private static final DynamicCommandExceptionType INVALID = new DynamicCommandExceptionType(
            value -> new LiteralMessage("Invalid experience amount: " + value)
    );

    private ExperienceAmounts() {
    }

    public static Value parse(String token) throws CommandSyntaxException {
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
        } catch (NumberFormatException failure) {
            throw INVALID.create(token);
        }
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
