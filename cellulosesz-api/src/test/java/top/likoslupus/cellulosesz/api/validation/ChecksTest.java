package top.likoslupus.cellulosesz.api.validation;

import org.junit.jupiter.api.Test;

import java.math.BigDecimal;

import static org.junit.jupiter.api.Assertions.*;

final class ChecksTest {

    @Test
    void returnsAcceptedValuesAndInclusiveBoundaries() {
        var text = "plain";
        assertSame(text, Checks.requireNonBlank(text, "text"));
        assertSame(text, Checks.requireMaxLength(text, 5, "text"));
        assertSame(text, Checks.requireNoControlCharacters(text, "text"));

        assertEquals(1, Checks.requirePositive(1, "count"));
        assertEquals(1L, Checks.requirePositive(1L, "count"));
        assertEquals(0.25D, Checks.requirePositive(0.25D, "ratio"));
        assertEquals(0, Checks.requireNonNegative(0, "count"));
        assertEquals(0L, Checks.requireNonNegative(0L, "count"));
        assertEquals(-0.0D, Checks.requireNonNegative(-0.0D, "ratio"));
        assertEquals(3, Checks.requireInRange(3, 3, 7, "count"));
        assertEquals(7, Checks.requireInRange(7, 3, 7, "count"));
        assertEquals(7L, Checks.requireInRange(7L, 3L, 7L, "count"));
        assertEquals(1.5D, Checks.requireInRange(1.5D, 1.5D, 2.5D, "ratio"));
        assertEquals(
                new BigDecimal("0.01"),
                Checks.requirePositive(new BigDecimal("0.01"), "amount")
        );
    }

    @Test
    void rejectsInvalidNumericValues() {
        assertThrows(IllegalArgumentException.class, () -> Checks.requirePositive(0, "count"));
        assertThrows(IllegalArgumentException.class, () -> Checks.requirePositive(-1L, "count"));
        assertThrows(IllegalArgumentException.class, () -> Checks.requirePositive(-0.0D, "ratio"));
        assertThrows(IllegalArgumentException.class, () -> Checks.requireNonNegative(-1, "count"));
        assertThrows(IllegalArgumentException.class, () -> Checks.requireFinite(Double.NaN, "ratio"));
        assertThrows(
                IllegalArgumentException.class,
                () -> Checks.requireFinite(Double.POSITIVE_INFINITY, "ratio")
        );
        assertThrows(
                IllegalArgumentException.class,
                () -> Checks.requireFinite(Float.NEGATIVE_INFINITY, "ratio")
        );
        assertThrows(
                IllegalArgumentException.class,
                () -> Checks.requireInRange(Double.NaN, 0.0D, 1.0D, "ratio")
        );
        assertThrows(
                IllegalArgumentException.class,
                () -> Checks.requireInRange(1, 2, 1, "count")
        );
    }

    @Test
    void rejectsBlankLongAndControlText() {
        assertThrows(IllegalArgumentException.class, () -> Checks.requireNonBlank("", "name"));
        assertThrows(IllegalArgumentException.class, () -> Checks.requireNonBlank(" \t", "name"));
        assertEquals("abc", Checks.requireMaxLength("abc", 3, "name"));
        assertThrows(
                IllegalArgumentException.class,
                () -> Checks.requireMaxLength("abcd", 3, "name")
        );
        assertThrows(
                IllegalArgumentException.class,
                () -> Checks.requireNoControlCharacters("a\nb", "command")
        );
        assertThrows(
                IllegalArgumentException.class,
                () -> Checks.requireNoControlCharacters("a\u0000b", "command")
        );
    }

    @Test
    void failuresIdentifyTheParameter() {
        var failure = assertThrows(
                IllegalArgumentException.class,
                () -> Checks.requireInRange(8, 3, 7, "count")
        );
        assertTrue(failure.getMessage().contains("count"));
        assertTrue(failure.getMessage().contains("[3, 7]"));
        assertTrue(failure.getMessage().contains("8"));
    }

}
