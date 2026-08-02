package top.likoslupus.cellulosesz.modules.economy.command.argument;

import com.mojang.brigadier.LiteralMessage;
import com.mojang.brigadier.StringReader;
import com.mojang.brigadier.arguments.ArgumentType;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.mojang.brigadier.exceptions.DynamicCommandExceptionType;

import java.math.BigDecimal;
import java.util.Collection;
import java.util.List;

import static top.likoslupus.cellulosesz.api.validation.NumericChecks.requireNonNegative;

import static java.util.Objects.requireNonNull;

public final class MoneyArgument implements ArgumentType<BigDecimal> {

    private static final DynamicCommandExceptionType INVALID = new DynamicCommandExceptionType(value -> new LiteralMessage(
            "Invalid money amount: " + value));
    private final int maximumScale;
    private final BigDecimal maximum;
    private final boolean positive;

    private MoneyArgument(
            int maximumScale,
            BigDecimal maximum,
            boolean positive
    ) {
        this.maximumScale = requireNonNegative(maximumScale, "maximumScale");
        this.maximum = requireNonNull(maximum, "maximum");
        this.positive = positive;
    }

    public static MoneyArgument nonNegative(int scale, BigDecimal maximum) {
        return new MoneyArgument(scale, maximum, false);
    }

    public static MoneyArgument positive(int scale, BigDecimal maximum) {
        return new MoneyArgument(scale, maximum, true);
    }

    public static BigDecimal get(CommandContext<?> context, String name) {
        return context.getArgument(name, BigDecimal.class);
    }

    @Override
    public BigDecimal parse(StringReader reader) throws CommandSyntaxException {
        var start = reader.getCursor();
        var token = reader.readUnquotedString();

        try {
            if (token.isBlank()
                    || token.startsWith("+")
                    || token.length() > 128
            ) {
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
            reader.setCursor(start);
            throw INVALID.createWithContext(reader, token);
        }
    }

    @Override
    public Collection<String> getExamples() {
        return List.of("10", "10.50");
    }

}
