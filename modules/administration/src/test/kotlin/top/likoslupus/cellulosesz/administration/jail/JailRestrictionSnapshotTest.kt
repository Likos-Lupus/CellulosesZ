package top.likoslupus.cellulosesz.administration.jail

import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test
import top.likoslupus.cellulosesz.administration.config.JailSettings

class JailRestrictionSnapshotTest {

    private fun snapshot(
        enabled: Boolean = true,
        allowed: List<String> = listOf("helpop"),
    ): JailRestrictionSnapshot =
        JailCommandAccessPolicy.snapshotFrom(
            JailSettings(
                restrictCommands = enabled,
                allowedCommands = allowed
            )
        )

    @Test
    fun `a plain allowed command is accepted in canonical and namespaced form`() {
        val snapshot = snapshot()

        assertTrue(snapshot.isAllowed("helpop"))
        assertTrue(snapshot.isAllowed("/helpop"))
        assertTrue(snapshot.isAllowed("  /helpop  reason here"))
        assertTrue(snapshot.isAllowed("cellulosesz:helpop"))
        assertTrue(snapshot.isAllowed("HELPOP"))
    }

    @Test
    fun `an unlisted command is denied`() {
        val snapshot = snapshot()

        assertFalse(snapshot.isAllowed("home"))
        assertFalse(snapshot.isAllowed("/tp 0 0 0"))
        assertFalse(snapshot.isAllowed("cellulosesz:home"))
    }

    @Test
    fun `blank or missing input is denied`() {
        val snapshot = snapshot()

        assertFalse(snapshot.isAllowed(null))
        assertFalse(snapshot.isAllowed("   "))
        assertFalse(snapshot.isAllowed("/"))
    }

    @Test
    fun `a disabled snapshot allows nothing through the policy`() {
        val snapshot = snapshot(enabled = false)

        assertFalse(snapshot.enabled)
        assertFalse(snapshot.isAllowed("helpop"))
    }

    @Test
    fun `a namespaced configured entry is accepted only in that form`() {
        val snapshot = snapshot(allowed = listOf("helpop", "cellulosesz:appeal"))

        assertTrue(snapshot.isAllowed("helpop"))
        assertTrue(snapshot.isAllowed("cellulosesz:appeal"))
        assertFalse(snapshot.isAllowed("appeal"))
    }

    @Test
    fun `the snapshot is immutable`() {
        val snapshot = snapshot()
        val before = snapshot.allowedRoutes

        assertEquals(before, snapshot.allowedRoutes)
    }

}
