package top.likoslupus.cellulosesz.api.validation;

import org.junit.jupiter.api.Test;

import java.math.BigDecimal;

import static org.junit.jupiter.api.Assertions.*;

final class ChecksTest {

    @Test
    void require_whenValueValid_returnsValue() {
        var text = "plain";
        assertSame(text, TextChecks.requireNonBlank(text, "text"));
        assertSame(text, TextChecks.requireMaxLength(text, 5, "text"));
        assertSame(text, TextChecks.requireNoControlCharacters(text, "text"));

        assertEquals(1, NumericChecks.requirePositive(1, "count"));
        assertEquals(1L, NumericChecks.requirePositive(1L, "count"));
        assertEquals(0.25D, NumericChecks.requirePositive(0.25D, "ratio"));
        assertEquals(0, NumericChecks.requireNonNegative(0, "count"));
        assertEquals(0L, NumericChecks.requireNonNegative(0L, "count"));
        assertEquals(-0.0D, NumericChecks.requireNonNegative(-0.0D, "ratio"));
        assertEquals(3, RangeChecks.requireInRange(3, 3, 7, "count"));
        assertEquals(7, RangeChecks.requireInRange(7, 3, 7, "count"));
        assertEquals(7L, RangeChecks.requireInRange(7L, 3L, 7L, "count"));
        assertEquals(1.5D, RangeChecks.requireInRange(1.5D, 1.5D, 2.5D, "ratio"));
        assertEquals(
                new BigDecimal("0.01"),
                NumericChecks.requirePositive(new BigDecimal("0.01"), "amount")
        );
    }

    @Test
    void numericChecks_whenValueInvalid_rejectValue() {
        assertThrows(
                IllegalArgumentException.class,
                () -> NumericChecks.requirePositive(0, "count")
        );
        assertThrows(
                IllegalArgumentException.class,
                () -> NumericChecks.requirePositive(-1L, "count")
        );
        assertThrows(
                IllegalArgumentException.class,
                () -> NumericChecks.requirePositive(-0.0D, "ratio")
        );
        assertThrows(
                IllegalArgumentException.class,
                () -> NumericChecks.requireNonNegative(-1, "count")
        );
        assertThrows(
                IllegalArgumentException.class,
                () -> NumericChecks.requireFinite(Double.NaN, "ratio")
        );
        assertThrows(
                IllegalArgumentException.class,
                () -> NumericChecks.requireFinite(Double.POSITIVE_INFINITY, "ratio")
        );
        assertThrows(
                IllegalArgumentException.class,
                () -> NumericChecks.requireFinite(Float.NEGATIVE_INFINITY, "ratio")
        );
        assertThrows(
                IllegalArgumentException.class,
                () -> RangeChecks.requireInRange(Double.NaN, 0.0D, 1.0D, "ratio")
        );
        assertThrows(
                IllegalArgumentException.class,
                () -> RangeChecks.requireInRange(1, 2, 1, "count")
        );
    }

    @Test
    void textChecks_whenValueInvalid_rejectValue() {
        assertThrows(
                IllegalArgumentException.class,
                () -> TextChecks.requireNonBlank("", "name")
        );
        assertThrows(
                IllegalArgumentException.class,
                () -> TextChecks.requireNonBlank(" \t", "name")
        );
        assertEquals(
                "abc",
                TextChecks.requireMaxLength("abc", 3, "name")
        );
        assertThrows(
                IllegalArgumentException.class,
                () -> TextChecks.requireMaxLength("abcd", 3, "name")
        );
        assertThrows(
                IllegalArgumentException.class,
                () -> TextChecks.requireNoControlCharacters("a\nb", "command")
        );
        assertThrows(
                IllegalArgumentException.class,
                () -> TextChecks.requireNoControlCharacters("a\u0000b", "command")
        );
    }

    @Test
    void require_whenValueInvalid_identifiesParameter() {
        var failure = assertThrows(
                IllegalArgumentException.class,
                () -> RangeChecks.requireInRange(8, 3, 7, "count")
        );

        assertTrue(failure.getMessage().contains("count"));
        assertTrue(failure.getMessage().contains("[3, 7]"));
        assertTrue(failure.getMessage().contains("8"));
    }

}
