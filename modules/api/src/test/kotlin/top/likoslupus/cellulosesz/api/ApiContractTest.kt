package top.likoslupus.cellulosesz.api

import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test
import java.util.*

class ApiContractTest {

    @Test
    fun `an empty player result is distinct from not-ready`() {
        val lookup = PlayerLookup { ApiQuery.Available(null) }

        val available = assertInstanceOf(
            ApiQuery.Available::class.java,
            lookup.findById(UUID.randomUUID()),
        )
        assertNull(available.value)
    }

    @Test
    fun `availability states are explicit`() {
        assertEquals(
            setOf("NOT_READY", "READY", "STOPPING", "STOPPED"),
            ApiAvailability.entries.map { it.name }.toSet(),
        )
    }

    @Test
    fun `origin kinds include an escape hatch for new internal causes`() {
        assertEquals("OTHER", TeleportOriginKind.OTHER.name)
    }

}
