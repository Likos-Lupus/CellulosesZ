package top.likoslupus.cellulosesz.home

import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test

class HomeNameTest {

    @Test
    fun `accepts lowercase alphanumeric underscore and dash`() {
        assertNotNull(HomeName.parse("home"))
        assertNotNull(HomeName.parse("base_1"))
        assertNotNull(HomeName.parse("my-home"))
        assertNotNull(HomeName.parse("a"))
    }

    @Test
    fun `rejects invalid names`() {
        assertNull(HomeName.parse(""))
        assertNull(HomeName.parse("Home"))
        assertNull(HomeName.parse("with space"))
        assertNull(HomeName.parse("../escape"))
        assertNull(HomeName.parse("a".repeat(33)))
    }

    @Test
    fun `keeps the original value`() {
        assertEquals("base_1", HomeName.parse("base_1")?.value)
    }

}
