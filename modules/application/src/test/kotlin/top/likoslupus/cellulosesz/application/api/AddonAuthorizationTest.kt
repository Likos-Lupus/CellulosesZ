package top.likoslupus.cellulosesz.application.api

import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import top.likoslupus.cellulosesz.api.actor.AddonAccess
import top.likoslupus.cellulosesz.api.actor.AddonDescriptor
import top.likoslupus.cellulosesz.application.api.extension.AddonMessageOverlay
import top.likoslupus.cellulosesz.application.api.extension.ApplicationExtensionHub
import top.likoslupus.cellulosesz.application.config.AddonIntegrationSettings
import top.likoslupus.cellulosesz.application.config.ApiIntegrationSettings

/**
 * A privileged economy operation must authorize against the exact handle the hub registered plus the
 * live capability config. An addon that simply implements `AddonAccess` itself — or that presents an
 * ungranted capability / disabled system — must never be treated as authorized.
 */
class AddonAuthorizationTest {

    private fun hub(settings: () -> ApiIntegrationSettings): ApplicationExtensionHub =
        ApplicationExtensionHub(
            ApiAuthorization(settings),
            AddonMessageOverlay()
        )

    private fun forged(id: String): AddonAccess =
        object : AddonAccess {
            override val addonId: String = id

            override fun isAllowed(capability: String): Boolean =
                true
        }

    @Test
    fun `a self-implemented access handle is rejected even with a granted capability`() {
        val hub = hub {
            ApiIntegrationSettings(
                enabled = true,
                addons = mapOf("quests" to AddonIntegrationSettings(listOf("economy.grant"))),
            )
        }
        val registered = hub.registerAddon(
            AddonDescriptor(
                "quests",
                "Quests"
            )
        )
        assertTrue(
            hub.isAuthorizedAccess(
                registered,
                "economy.grant"
            )
        )

        // Same id, forged handle, isAllowed()==true: still rejected because it is not the registered instance.
        assertFalse(
            hub.isAuthorizedAccess(
                forged("quests"),
                "economy.grant"
            )
        )
    }

    @Test
    fun `an unregistered addon and an ungranted capability are rejected`() {
        val hub = hub {
            ApiIntegrationSettings(
                enabled = true,
                addons = mapOf("quests" to AddonIntegrationSettings(listOf("economy.grant"))),
            )
        }
        val registered = hub.registerAddon(
            AddonDescriptor(
                "quests",
                "Quests"
            )
        )

        assertFalse(
            hub.isAuthorizedAccess(
                registered,
                "economy.set"
            )
        )
        assertFalse(
            hub.isAuthorizedAccess(
                forged("unknown-mod"),
                "economy.grant"
            )
        )
    }

    @Test
    fun `every capability is denied while the system is disabled`() {
        val hub = hub {
            ApiIntegrationSettings(
                enabled = false,
                addons = mapOf("quests" to AddonIntegrationSettings(listOf("economy.grant"))),
            )
        }
        val registered = hub.registerAddon(
            AddonDescriptor(
                "quests",
                "Quests"
            )
        )
        assertFalse(
            hub.isAuthorizedAccess(
                registered,
                "economy.grant"
            )
        )
    }

}
