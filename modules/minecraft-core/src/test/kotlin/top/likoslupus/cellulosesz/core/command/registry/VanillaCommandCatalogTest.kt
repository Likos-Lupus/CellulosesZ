package top.likoslupus.cellulosesz.core.command.registry

import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test

class VanillaCommandCatalogTest {

    @Test
    fun `fallback literals are the single-segment cz prefix, never a cz root`() {
        assertEquals("czme", VanillaCommandId.ME.fallbackLiteral)
        assertEquals("cztp", VanillaCommandId.TP.fallbackLiteral)
        assertEquals("czkill", VanillaCommandId.KILL.fallbackLiteral)
        assertEquals("czgamemode", VanillaCommandId.GAMEMODE.fallbackLiteral)
    }

    @Test
    fun `only verified targets may be configured for takeover`() {
        assertNull(VanillaCommandCatalog.takeoverProblem("me"))

        assertNotNull(VanillaCommandCatalog.takeoverProblem("tp"))
        assertNotNull(VanillaCommandCatalog.takeoverProblem("kill"))
        assertNotNull(VanillaCommandCatalog.takeoverProblem("gamemode"))
        assertNotNull(VanillaCommandCatalog.takeoverProblem("nope"))
    }

    @Test
    fun `the tp override carries an extra permission but me does not`() {
        assertNull(VanillaCommandCatalog.overridePermission(VanillaCommandId.ME))
        assertEquals(
            "cellulosesz.command.override.tp",
            VanillaCommandCatalog.overridePermission(VanillaCommandId.TP)?.node,
        )
    }

    @Test
    fun `takeover requires feature, global switch and allowlist`() {
        assertEquals(
            VanillaRouteMode.FEATURE_DISABLED,
            effectiveVanillaRoute(
                VanillaCommandId.ME,
                featureEnabled = false,
                overridesEnabled = true,
                overrides = setOf("me")
            ).mode,
        )
        assertEquals(
            VanillaRouteMode.PRESERVE_VANILLA,
            effectiveVanillaRoute(
                VanillaCommandId.ME,
                featureEnabled = true,
                overridesEnabled = false,
                overrides = setOf("me")
            ).mode,
        )
        assertEquals(
            VanillaRouteMode.TAKE_OVER_VANILLA,
            effectiveVanillaRoute(
                VanillaCommandId.ME,
                featureEnabled = true,
                overridesEnabled = true,
                overrides = emptySet()
            ).mode,
        )
        assertEquals(
            VanillaRouteMode.TAKE_OVER_VANILLA,
            effectiveVanillaRoute(
                VanillaCommandId.ME,
                featureEnabled = true,
                overridesEnabled = true,
                overrides = setOf("me")
            ).mode,
        )
        assertEquals(
            VanillaRouteMode.PRESERVE_VANILLA,
            effectiveVanillaRoute(
                VanillaCommandId.TP,
                featureEnabled = true,
                overridesEnabled = true,
                overrides = setOf("me")
            ).mode,
        )
    }

    @Test
    fun `catalog lookup maps canonical names to their conflict`() {
        assertEquals(
            VanillaCommandId.TP,
            VanillaCommandCatalog.forCanonical("tp")
        )
        assertNull(VanillaCommandCatalog.forCanonical("home"))
    }

}
