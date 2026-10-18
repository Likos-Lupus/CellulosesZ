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

    @Test
    fun `new moderation and adjustment bounds are validated`() {
        assertTrue(
            "administration.moderation.loginAttackDelaySeconds" in
                    paths(
                        AdministrationSettings(
                            moderation = ModerationSettings(loginAttackDelaySeconds = -1)
                        )
                    )
        )
        assertTrue(
            "administration.playerAdjustments.healCooldownSeconds" in
                    paths(
                        AdministrationSettings(
                            playerAdjustments = PlayerAdjustmentSettings(healCooldownSeconds = -1)
                        )
                    )
        )
        assertTrue(
            "administration.playerAdjustments.noGodWorlds" in
                    paths(
                        AdministrationSettings(
                            playerAdjustments = PlayerAdjustmentSettings(
                                noGodWorlds = listOf("overworld")
                            )
                        )
                    )
        )
        assertTrue(
            "administration.entityTools.maxSpawnAmount" in
                    paths(
                        AdministrationSettings(
                            entityTools = EntityToolsSettings(maxSpawnAmount = 0)
                        )
                    )
        )
        assertTrue(
            "administration.worldTools.maxFireworkPower" in
                    paths(
                        AdministrationSettings(
                            worldTools = WorldToolsSettings(maxFireworkPower = 0)
                        )
                    )
        )
        assertTrue(
            "administration.signEditing.maxLineLength" in
                    paths(
                        AdministrationSettings(
                            signEditing = SignEditingSettings(maxLineLength = 0)
                        )
                    )
        )
        assertTrue(
            "administration.signEditing.maxDistance" in
                    paths(
                        AdministrationSettings(
                            signEditing = SignEditingSettings(maxDistance = 0.0)
                        )
                    )
        )
    }

}
