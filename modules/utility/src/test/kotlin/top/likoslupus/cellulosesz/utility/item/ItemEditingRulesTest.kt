package top.likoslupus.cellulosesz.utility.item

import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test

class ItemEditingRulesTest {

    @Test
    fun `a name within the limit is valid`() {
        assertEquals(
            ItemEditingRules.validateName("Blade of the Dawn", 64),
            NameValidation.Valid("Blade of the Dawn")
        )
    }

    @Test
    fun `a name over the limit is rejected with the limit`() {
        assertEquals(
            NameValidation.TooLong(64),
            ItemEditingRules.validateName("x".repeat(65), 64)
        )
    }

    @Test
    fun `lore splits on the pipe separator`() {
        assertEquals(
            ItemEditingRules.parseLore("one|two", 10, 80),
            LoreValidation.Valid(listOf("one", "two"))
        )
    }

    @Test
    fun `too many lore lines is rejected`() {
        assertEquals(
            ItemEditingRules.parseLore("a|b|c", 2, 80),
            LoreValidation.TooManyLines(2)
        )
    }

    @Test
    fun `an over-long lore line is rejected`() {
        assertEquals(
            ItemEditingRules.parseLore("abcd", 10, 3),
            LoreValidation.LineTooLong(3)
        )
    }

    @Test
    fun `hat is allowed for equipment-capable items or when any item is allowed`() {
        assertTrue(
            ItemEditingRules.isHatAllowed(
                equipmentCapable = true,
                allowAnyItemAsHat = false
            )
        )
        assertTrue(
            ItemEditingRules.isHatAllowed(
                equipmentCapable = false,
                allowAnyItemAsHat = true
            )
        )
        assertFalse(
            ItemEditingRules.isHatAllowed(
                equipmentCapable = false,
                allowAnyItemAsHat = false
            )
        )
    }

}
