package top.likoslupus.cellulosesz.api.user

import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test
import java.util.*

class UserContractsTest {

    @Test
    fun cellUser_defaultsAndWither() {
        val uuid = UUID.randomUUID()
        val user = CellUser.create(uuid)
        assertEquals(uuid, user.uuid)
        assertNull(user.lastKnownName)
        assertFalse(user.state.god)
        assertTrue(user.preferences.privateMessages)

        val updated = user.withLastKnownName("TestPlayer")
        assertEquals("TestPlayer", updated.lastKnownName)
        assertEquals(uuid, updated.uuid)
    }

    @Test
    fun userPreferences_withers() {
        val prefs = UserPreferences.defaults()
        assertTrue(prefs.privateMessages)
        assertFalse(prefs.socialSpy)

        val updated = prefs.withSocialSpy(true).withPrivateMessages(false)
        assertTrue(updated.socialSpy)
        assertFalse(updated.privateMessages)
    }

}
