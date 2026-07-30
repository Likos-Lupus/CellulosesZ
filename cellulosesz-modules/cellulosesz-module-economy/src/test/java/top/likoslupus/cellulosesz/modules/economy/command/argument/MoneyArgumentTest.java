package top.likoslupus.cellulosesz.modules.economy.command.argument;

import com.mojang.brigadier.StringReader;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

final class MoneyArgumentTest {

    @Test
    void preservesExactDecimalValue() throws Exception {
        assertEquals(
                new BigDecimal("10.50"),
                MoneyArgument.positive(
                        2,
                        new BigDecimal("1000.00")
                ).parse(new StringReader("10.50"))
        );
    }

    @Test
    void enforcesSignScaleAndMaximum() {
        var positive = MoneyArgument.positive(2, new BigDecimal("1000.00"));
        assertThrows(Exception.class, () -> positive.parse(new StringReader("0")));
        assertThrows(Exception.class, () -> positive.parse(new StringReader("-1")));
        assertThrows(Exception.class, () -> positive.parse(new StringReader("1.001")));
        assertThrows(Exception.class, () -> positive.parse(new StringReader("1000.01")));
        assertThrows(Exception.class, () -> positive.parse(new StringReader("NaN")));
        assertThrows(Exception.class, () -> positive.parse(new StringReader("Infinity")));
    }

    @Test
    void nonNegativeAcceptsZeroButRejectsPlusPrefix() throws Exception {
        var amount = MoneyArgument.nonNegative(2, BigDecimal.TEN);
        assertEquals(BigDecimal.ZERO, amount.parse(new StringReader("0")));
        assertThrows(Exception.class, () -> amount.parse(new StringReader("+1")));
    }

}
