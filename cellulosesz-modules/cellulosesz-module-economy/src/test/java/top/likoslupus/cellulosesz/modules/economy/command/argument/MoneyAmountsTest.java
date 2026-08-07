package top.likoslupus.cellulosesz.modules.economy.command.argument;

import com.mojang.brigadier.exceptions.CommandSyntaxException;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

final class MoneyAmountsTest {

    @Test
    void parse_whenDecimalProvided_preservesExactValue() throws Exception {
        assertEquals(
                new BigDecimal("10.50"),
                MoneyAmounts.positive("10.50", 2, new BigDecimal("1000.00"))
        );
    }

    @Test
    void parse_whenSignScaleOrMaximumInvalid_rejects() {
        var maximum = new BigDecimal("1000.00");
        assertThrows(
                CommandSyntaxException.class,
                () -> MoneyAmounts.positive("0", 2, maximum)
        );
        assertThrows(
                CommandSyntaxException.class,
                () -> MoneyAmounts.positive("-1", 2, maximum)
        );
        assertThrows(
                CommandSyntaxException.class,
                () -> MoneyAmounts.positive("1.001", 2, maximum)
        );
        assertThrows(
                CommandSyntaxException.class,
                () -> MoneyAmounts.positive("1000.01", 2, maximum)
        );
        assertThrows(
                CommandSyntaxException.class,
                () -> MoneyAmounts.positive("NaN", 2, maximum)
        );
        assertThrows(
                CommandSyntaxException.class,
                () -> MoneyAmounts.positive("Infinity", 2, maximum)
        );
    }

    @Test
    void parseNonNegative_withZeroOrPlusPrefix_acceptsZeroOnly() throws Exception {
        assertEquals(
                BigDecimal.ZERO,
                MoneyAmounts.nonNegative("0", 2, BigDecimal.TEN)
        );
        assertThrows(
                CommandSyntaxException.class,
                () -> MoneyAmounts.nonNegative("+1", 2, BigDecimal.TEN)
        );
    }

}
