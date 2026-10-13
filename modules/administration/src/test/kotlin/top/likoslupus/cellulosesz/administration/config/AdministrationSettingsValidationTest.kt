package top.likoslupus.cellulosesz.administration.config

import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

class AdministrationSettingsValidationTest {

    private fun paths(settings: AdministrationSettings): Set<String> =
        AdministrationSettingsValidation.validate(settings).map { it.path }.toSet()

    @Test
    fun `defaults are valid`() {
        assertTrue(
            AdministrationSettingsValidation.validate(
                AdministrationSettings()
            ).isEmpty()
        )
    }

    @Test
    fun `blank default jail is allowed and a bad name is rejected`() {
        assertTrue(
            paths(
                AdministrationSettings(
                    jail = JailSettings(defaultJail = "")
                )
            ).isEmpty()
        )
        assertTrue(
            "administration.jail.defaultJail" in
                    paths(
                        AdministrationSettings(
                            jail = JailSettings(defaultJail = "Bad Name")
                        )
                    )
        )
    }

    @Test
    fun `jail limits are bounded`() {
        assertTrue(
            "administration.jail.maxJails" in
                    paths(
                        AdministrationSettings(
                            jail = JailSettings(maxJails = 0)
                        )
                    )
        )
        assertTrue(
            "administration.jail.allowedRadius" in
                    paths(
                        AdministrationSettings(
                            jail = JailSettings(allowedRadius = -1.0)
                        )
                    )
        )
        assertTrue(
            "administration.jail.allowedRadius" in
                    paths(
                        AdministrationSettings(
                            jail = JailSettings(allowedRadius = Double.NaN)
                        )
                    )
        )
        assertTrue(
            "administration.jail.confinementCheckIntervalTicks" in
                    paths(
                        AdministrationSettings(
                            jail = JailSettings(confinementCheckIntervalTicks = 0)
                        )
                    )
        )
    }

    @Test
    fun `player adjustment bounds are validated`() {
        assertTrue(
            "administration.playerAdjustments.maximumExperienceAdjustment" in
                    paths(
                        AdministrationSettings(
                            playerAdjustments = PlayerAdjustmentSettings(maximumExperienceAdjustment = 0)
                        )
                    )
        )
        assertTrue(
            "administration.playerAdjustments.minimumSpeedMultiplier" in
                    paths(
                        AdministrationSettings(
                            playerAdjustments = PlayerAdjustmentSettings(minimumSpeedMultiplier = -1.0)
                        )
                    )
        )
        assertTrue(
            "administration.playerAdjustments.maximumSpeedMultiplier" in
                    paths(
                        AdministrationSettings(
                            playerAdjustments = PlayerAdjustmentSettings(maximumSpeedMultiplier = 20.0)
                        )
                    )
        )
        assertTrue(
            "administration.playerAdjustments.minimumSpeedMultiplier" in
                    paths(
                        AdministrationSettings(
                            playerAdjustments = PlayerAdjustmentSettings(
                                minimumSpeedMultiplier = 3.0,
                                maximumSpeedMultiplier = 1.0,
                            )
                        )
                    )
        )
    }

}
