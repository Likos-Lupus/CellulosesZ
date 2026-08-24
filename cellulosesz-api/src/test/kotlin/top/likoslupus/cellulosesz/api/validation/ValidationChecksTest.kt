package top.likoslupus.cellulosesz.api.validation

import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test
import java.math.BigDecimal
import java.time.Duration

class ValidationChecksTest {

    @Test
    fun `require when value valid returns value`() {
        val text = "plain"
        assertSame(text, text.requireNonBlank { "text" })
        assertSame(text, requireNonBlank(text, "text"))
        assertSame(text, text.requireMaxLength(5) { "text" })
        assertSame(text, requireMaxLength(text, 5, "text"))
        assertSame(text, text.requireNoControlCharacters { "text" })
        assertSame(text, requireNoControlCharacters(text, "text"))

        assertEquals(1, 1.requirePositive { "count" })
        assertEquals(1, requirePositive(1, "count"))
        assertEquals(1L, 1L.requirePositive { "count" })
        assertEquals(1L, requirePositive(1L, "count"))
        assertEquals(0.25, 0.25.requirePositive { "ratio" })
        assertEquals(0.25, requirePositive(0.25, "ratio"))
        assertEquals(0, 0.requireNonNegative { "count" })
        assertEquals(0, requireNonNegative(0, "count"))
        assertEquals(0L, 0L.requireNonNegative { "count" })
        assertEquals(0L, requireNonNegative(0L, "count"))
        assertEquals(-0.0, (-0.0).requireNonNegative { "ratio" })
        assertEquals(-0.0, requireNonNegative(-0.0, "ratio"))
        assertEquals(3, 3.requireInRange(3, 7) { "count" })
        assertEquals(7, 7.requireInRange(3, 7) { "count" })
        assertEquals(7L, 7L.requireInRange(3L, 7L) { "count" })
        assertEquals(1.5, 1.5.requireInRange(1.5, 2.5) { "ratio" })
        assertEquals(BigDecimal("0.01"), BigDecimal("0.01").requirePositive { "amount" })
        assertEquals(Duration.ofSeconds(1), Duration.ofSeconds(1).requirePositive { "duration" })
        assertEquals(Duration.ZERO, Duration.ZERO.requireNonNegative { "duration" })

        assertEquals(-1, (-1).requirePositiveOrNegativeOne { "sentinel" })
        assertEquals(5, 5.requirePositiveOrNegativeOne { "sentinel" })
        assertEquals(-1L, (-1L).requirePositiveOrNegativeOne { "sentinel" })
        assertEquals(5L, 5L.requirePositiveOrNegativeOne { "sentinel" })

        val list = listOf("a", "b")
        assertSame(list, list.requireNonEmpty { "list" })
        assertSame(list, requireNonEmpty(list, "list"))

        val map = mapOf("k" to "v")
        assertSame(map, map.requireNonEmpty { "map" })
        assertSame(map, requireNonEmpty(map, "map"))
    }

    @Test
    fun `numeric checks when value invalid reject value`() {
        assertThrows(IllegalArgumentException::class.java) {
            0.requirePositive { "count" }
        }
        assertThrows(IllegalArgumentException::class.java) {
            (-1L).requirePositive { "count" }
        }
        assertThrows(IllegalArgumentException::class.java) {
            (-0.0).requirePositive { "ratio" }
        }
        assertThrows(IllegalArgumentException::class.java) {
            (-1).requireNonNegative { "count" }
        }
        assertThrows(IllegalArgumentException::class.java) {
            Double.NaN.requireFinite { "ratio" }
        }
        assertThrows(IllegalArgumentException::class.java) {
            Double.POSITIVE_INFINITY.requireFinite { "ratio" }
        }
        assertThrows(IllegalArgumentException::class.java) {
            Float.NEGATIVE_INFINITY.requireFinite { "ratio" }
        }
        assertThrows(IllegalArgumentException::class.java) {
            Double.NaN.requireInRange(0.0, 1.0) { "ratio" }
        }
        assertThrows(IllegalArgumentException::class.java) {
            1.requireInRange(2, 1) { "count" }
        }
        assertThrows(IllegalArgumentException::class.java) {
            0.requirePositiveOrNegativeOne { "sentinel" }
        }
        assertThrows(IllegalArgumentException::class.java) {
            (-2).requirePositiveOrNegativeOne { "sentinel" }
        }
    }

    @Test
    fun `text checks when value invalid reject value`() {
        assertThrows(IllegalArgumentException::class.java) {
            "".requireNonBlank { "name" }
        }
        assertThrows(IllegalArgumentException::class.java) {
            " \t".requireNonBlank { "name" }
        }
        assertThrows(IllegalArgumentException::class.java) {
            "".requireNonEmpty { "name" }
        }
        assertEquals("abc", "abc".requireMaxLength(3) { "name" })
        assertThrows(IllegalArgumentException::class.java) {
            "abcd".requireMaxLength(3) { "name" }
        }
        assertThrows(IllegalArgumentException::class.java) {
            "a".requireMinLength(2) { "name" }
        }
        assertThrows(IllegalArgumentException::class.java) {
            "a\nb".requireNoControlCharacters { "command" }
        }
        assertThrows(IllegalArgumentException::class.java) {
            "a\u0000b".requireNoControlCharacters { "command" }
        }
    }

    @Test
    fun `collection checks when empty reject value`() {
        assertThrows(IllegalArgumentException::class.java) {
            emptyList<String>().requireNonEmpty { "list" }
        }
        assertThrows(IllegalArgumentException::class.java) {
            emptyMap<String, String>().requireNonEmpty { "map" }
        }
    }

    @Test
    fun `range checks boundaries and comparison`() {
        assertEquals(5, 5.requireGreaterThan(4) { "val" })
        assertThrows(IllegalArgumentException::class.java) {
            4.requireGreaterThan(4) { "val" }
        }
        assertEquals(5, 5.requireAtLeast(5) { "val" })
        assertThrows(IllegalArgumentException::class.java) {
            4.requireAtLeast(5) { "val" }
        }
        assertEquals(4, 4.requireLessThan(5) { "val" })
        assertThrows(IllegalArgumentException::class.java) {
            5.requireLessThan(5) { "val" }
        }
        assertEquals(5, 5.requireAtMost(5) { "val" })
        assertThrows(IllegalArgumentException::class.java) {
            6.requireAtMost(5) { "val" }
        }
    }

    @Test
    fun `lazy message is evaluated only on failure`() {
        var evaluated = false
        val lazyName = {
            evaluated = true
            "testProperty"
        }

        // On success: must NOT evaluate lambda
        42.requirePositive(lazyName)
        assertEquals(
            false,
            evaluated,
            "Lazy message lambda should not be evaluated on success"
        )

        // On failure: must evaluate lambda
        val error = assertThrows(IllegalArgumentException::class.java) {
            (-5).requirePositive(lazyName)
        }
        assertEquals(
            true,
            evaluated,
            "Lazy message lambda should be evaluated on failure"
        )
        assertTrue(error.message?.contains("testProperty") == true)
    }

    @Test
    fun `require when value invalid identifies parameter`() {
        val failure = assertThrows(IllegalArgumentException::class.java) {
            8.requireInRange(3, 7) { "count" }
        }
        assertTrue(failure.message?.contains("count") == true)
        assertTrue(failure.message?.contains("[3, 7]") == true)
        assertTrue(failure.message?.contains("8") == true)
    }

}
