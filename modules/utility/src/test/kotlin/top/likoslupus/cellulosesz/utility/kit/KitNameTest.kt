package top.likoslupus.cellulosesz.utility.kit

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Test

class KitNameTest {

    @Test
    fun `accepts canonical names`() {
        listOf("starter", "a_b", "a-b", "a1").forEach { raw ->
            assertEquals(raw, KitName.parse(raw)?.value)
        }
    }

    @Test
    fun `canonicalizes case and trims`() {
        assertEquals(
            "starter",
            KitName.parse("Starter")?.value
        )
        assertEquals(
            "starter",
            KitName.parse("STARTER")?.value
        )
        assertEquals(
            "starter",
            KitName.parse("  starter  ")?.value
        )
    }

    @Test
    fun `accepts exactly 32 characters`() {
        val name = "a".repeat(32)

        assertEquals(
            name,
            KitName.parse(name)?.value
        )
    }

    @Test
    fun `rejects invalid names`() {
        listOf(
            "",
            " ",
            "a b",
            "a.b",
            "a/b",
            "a".repeat(33),
            "könig",
            "\u0000"
        ).forEach { raw ->
            assertNull(
                KitName.parse(raw),
                "expected '$raw' to be rejected"
            )
        }
    }

}
