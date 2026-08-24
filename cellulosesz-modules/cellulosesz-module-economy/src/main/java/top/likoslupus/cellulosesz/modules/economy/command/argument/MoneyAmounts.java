package top.likoslupus.cellulosesz.modules.economy.command.argument;

import com.mojang.brigadier.LiteralMessage;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.mojang.brigadier.exceptions.DynamicCommandExceptionType;

import java.math.BigDecimal;

import static top.likoslupus.cellulosesz.api.validation.Checks.requireNonNegative;

import static java.util.Objects.requireNonNull;

public final class MoneyAmounts {

    private static final DynamicCommandExceptionType INVALID = new DynamicCommandExceptionType(
            value -> new LiteralMessage("Invalid money amount: " + value)
    );

    private MoneyAmounts() {
    }

    public static BigDecimal nonNegative(
            String token,
            int maximumScale,
            BigDecimal maximum
    ) throws CommandSyntaxException {
        return parse(token, maximumScale, maximum, false);
    }

    private static BigDecimal parse(
            String token,
            int maximumScale,
            BigDecimal maximum,
            boolean positive
    ) throws CommandSyntaxException {
        requireNonNegative(maximumScale, "maximumScale");
        requireNonNull(maximum, "maximum");

        try {
            if (token.isBlank() || token.startsWith("+") || token.length() > 128) {
                throw new NumberFormatException();
            }

            var value = new BigDecimal(token);
            if (
                    (
                            positive
                                    ? value.signum() <= 0
                                    : value.signum() < 0
                    )
                            || value.scale() > maximumScale
                            || value.compareTo(maximum) > 0
            ) {
                throw new NumberFormatException();
            }
            return value;
        } catch (NumberFormatException failure) {
            throw INVALID.create(token);
        }
    }

    public static BigDecimal positive(
            String token,
            int maximumScale,
            BigDecimal maximum
    ) throws CommandSyntaxException {
        return parse(token, maximumScale, maximum, true);
    }

}
