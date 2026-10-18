package top.likoslupus.cellulosesz.utility.powertool

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

class PowertoolSpecTest {

    @Test
    fun `bare command binds to both clicks`() {
        assertEquals(
            PowertoolSpec.parse("spawn"),
            PowertoolSpec.Bound(PowertoolTrigger.BOTH, "spawn"),
        )
    }

    @Test
    fun `left and right prefixes select the trigger`() {
        assertEquals(
            PowertoolSpec.parse("l:home"),
            PowertoolSpec.Bound(PowertoolTrigger.LEFT, "home"),
        )
        assertEquals(
            PowertoolSpec.parse("r:workbench"),
            PowertoolSpec.Bound(PowertoolTrigger.RIGHT, "workbench"),
        )
        assertEquals(
            PowertoolSpec.parse("c:heal"),
            PowertoolSpec.Bound(PowertoolTrigger.BOTH, "heal"),
        )
    }

    @Test
    fun `a leading slash is stripped`() {
        assertEquals(
            PowertoolSpec.parse("/spawn"),
            PowertoolSpec.Bound(PowertoolTrigger.BOTH, "spawn"),
        )
    }

    @Test
    fun `blank input clears the binding`() {
        assertEquals(PowertoolSpec.Clear, PowertoolSpec.parse("   "))
    }

    @Test
    fun `a prefix with no command is invalid`() {
        assertEquals(PowertoolSpec.Invalid, PowertoolSpec.parse("l:"))
    }

    @Test
    fun `powertool commands are rejected as recursion`() {
        assertTrue(PowertoolSpec.parse("powertool spawn") is PowertoolSpec.Recursive)
        assertTrue(PowertoolSpec.parse("cellulosesz:powertool spawn") is PowertoolSpec.Recursive)
        assertTrue(PowertoolSpec.isRecursive("powertoollist"))
    }

}
