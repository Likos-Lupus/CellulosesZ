package top.likoslupus.cellulosesz.api.validation;

import org.junit.jupiter.api.Test;

import java.math.BigDecimal;

import static org.junit.jupiter.api.Assertions.*;

final class ChecksTest {

    @Test
    void acceptsInclusiveBoundaries() {
        assertEquals(0, Checks.requireNonNegative(0, "count"));
        assertEquals(1, Checks.requirePositive(1, "count"));
        assertEquals(3, Checks.requireInRange(3, 3, 7, "count"));
        assertEquals(7L, Checks.requireInRange(7L, 3L, 7L, "count"));
        assertEquals(1.5D, Checks.requireInRange(1.5D, 1.5D, 2.5D, "ratio"));
        assertEquals(new BigDecimal("0.01"), Checks.requirePositive(new BigDecimal("0.01"), "amount"));
        assertEquals(0.0D, Checks.requireNonNegative(0.0D, "ratio"));
        assertEquals(0.25D, Checks.requirePositive(0.25D, "ratio"));
        assertEquals(3, Checks.requireInRange(3, 3, 7, "count"));
        assertEquals(7L, Checks.requireInRange(7L, 3L, 7L, "count"));
        assertEquals(1.5D, Checks.requireInRange(1.5D, 1.5D, 2.5D, "ratio"));
        Checks.requireState(true, "ready");
    }

    @Test
    void messagesIdentifyNameAndRange() {
        var failure = assertThrows(
                IllegalArgumentException.class,
                () -> Checks.requireInRange(8, 3, 7, "count")
        );
        assertTrue(failure.getMessage().contains("count"));
        assertTrue(failure.getMessage().contains("[3, 7]"));
        assertTrue(failure.getMessage().contains("8"));
    }

    @Test
    void rejectsBlankNonFiniteLongAndControlText() {
        assertThrows(IllegalArgumentException.class, () -> Checks.requireNonBlank(" \t", "name"));
        assertThrows(IllegalArgumentException.class, () -> Checks.requireFinite(Double.NaN, "ratio"));
        assertThrows(IllegalArgumentException.class, () -> Checks.requireNonNegative(Double.NEGATIVE_INFINITY, "ratio"));
        assertThrows(IllegalArgumentException.class, () -> Checks.requirePositive(Double.NaN, "ratio"));
        assertThrows(IllegalArgumentException.class, () -> Checks.requireInRange(Double.POSITIVE_INFINITY, 0.0D, 1.0D, "ratio"));
        assertThrows(IllegalStateException.class, () -> Checks.requireState(false, "not ready"));
        assertThrows(IllegalArgumentException.class, () -> Checks.requireMaxLength("abcd", 3, "name"));
        assertThrows(IllegalArgumentException.class, () -> Checks.requireNoControlCharacters("a\nb", "command"));
        assertEquals("plain", Checks.requireNoControlCharacters("plain", "command"));
    }

}
