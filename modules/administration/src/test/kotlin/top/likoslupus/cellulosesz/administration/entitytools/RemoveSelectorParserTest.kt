package top.likoslupus.cellulosesz.administration.entitytools

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Test

class RemoveSelectorParserTest {

    @Test
    fun `named selectors win over entity type ids`() {
        assertEquals(
            RemoveSelectorParser.parse("all"),
            RemoveTarget.Category(EntitySelector.ALL),
        )
        assertEquals(
            RemoveSelectorParser.parse("ITEMFRAMES"),
            RemoveTarget.Category(EntitySelector.ITEM_FRAMES),
        )
    }

    @Test
    fun `bare entity ids are namespaced`() {
        assertEquals(
            RemoveSelectorParser.parse("zombie"),
            RemoveTarget.EntityType("minecraft:zombie"),
        )
    }

    @Test
    fun `namespaced entity ids are kept verbatim`() {
        assertEquals(
            RemoveSelectorParser.parse("cellulosesz:custom"),
            RemoveTarget.EntityType("cellulosesz:custom"),
        )
    }

    @Test
    fun `blank and malformed selectors are rejected`() {
        assertNull(RemoveSelectorParser.parse("   "))
        assertNull(RemoveSelectorParser.parse("not a selector!"))
    }

}
