package top.likoslupus.cellulosesz.api.item

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

class ItemContractsTest {

    @Test
    fun itemDescriptor_normalization() {
        val desc = ItemDescriptor("diamond", 5)
        assertEquals("minecraft:diamond", desc.item)
        assertEquals(5, desc.count)
        assertEquals("minecraft:diamond", desc.argument)

        val namespaced = ItemDescriptor("mod:custom_item", 1)
        assertEquals("mod:custom_item", namespaced.item)
    }

    @Test
    fun itemGrantResult_complete() {
        val full = ItemGrantResult(5, 5)
        assertTrue(full.complete())
    }

}
