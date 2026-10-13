package top.likoslupus.cellulosesz.utility.config

import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

class UtilitySettingsValidationTest {

    @Test
    fun `accepts defaults`() {
        assertTrue(UtilitySettingsValidation.validate(UtilitySettings()).isEmpty())
    }

    @Test
    fun `rejects non positive kit cap`() {
        val errors = UtilitySettingsValidation.validate(
            UtilitySettings(kits = KitSettings(maxKits = 0))
        )

        assertTrue(errors.any { it.path == "utility.kits.maxKits" })
    }

    @Test
    fun `rejects non positive item cap`() {
        val errors = UtilitySettingsValidation.validate(
            UtilitySettings(kits = KitSettings(maxItemsPerKit = 0))
        )

        assertTrue(errors.any { it.path == "utility.kits.maxItemsPerKit" })
    }

    @Test
    fun `rejects non positive cooldown`() {
        val errors = UtilitySettingsValidation.validate(
            UtilitySettings(kits = KitSettings(maxCooldownSeconds = 0))
        )

        assertTrue(errors.any { it.path == "utility.kits.maxCooldownSeconds" })
    }

    @Test
    fun `accepts null cooldown`() {
        assertTrue(
            UtilitySettingsValidation
                    .validate(
                        UtilitySettings(
                            kits = KitSettings(
                                maxCooldownSeconds = null
                            )
                        )
                    )
                    .isEmpty()
        )
    }

    @Test
    fun `rejects invalid item editing caps`() {
        val errors = UtilitySettingsValidation.validate(
            UtilitySettings(
                itemEditing = ItemEditingSettings(
                    maxItemNameLength = 0,
                    maxLoreLines = 0,
                    maxLoreLineLength = 0,
                    clearConfirmationSeconds = 0,
                )
            )
        ).map { it.path }.toSet()

        assertTrue("utility.itemEditing.maxItemNameLength" in errors)
        assertTrue("utility.itemEditing.maxLoreLines" in errors)
        assertTrue("utility.itemEditing.maxLoreLineLength" in errors)
        assertTrue("utility.itemEditing.clearConfirmationSeconds" in errors)
    }

}
