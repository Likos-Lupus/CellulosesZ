package top.likoslupus.cellulosesz.movement.request

import kotlinx.coroutines.runBlocking
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test
import top.likoslupus.cellulosesz.movement.config.TeleportRequestSettings
import java.util.*

class TpAutoServiceTest {

    private val owner = UUID.randomUUID()
    private val alice = UUID.randomUUID()
    private val bob = UUID.randomUUID()

    private var settings = TeleportRequestSettings()
    private val repository = FakeRepository()
    private val service = TpAutoService(repository, { settings })

    private class FakeRepository : TpAutoRepository {

        var snapshot = TpAutoSnapshot(emptyMap(), emptyMap())
        var failing = false
        val modeWrites = mutableListOf<Pair<UUID, TpAutoMode>>()
        val allowedWrites = mutableListOf<Pair<UUID, UUID>>()

        override fun loadAllBlocking(): TpAutoSnapshot = snapshot

        override suspend fun setMode(owner: UUID, mode: TpAutoMode) {
            failIfNeeded()
            modeWrites += owner to mode
        }

        override suspend fun addAllowed(owner: UUID, allowed: UUID) {
            failIfNeeded()
            allowedWrites += owner to allowed
        }

        override suspend fun removeAllowed(owner: UUID, allowed: UUID): Boolean {
            failIfNeeded()
            return true
        }

        private fun failIfNeeded() {
            if (failing) {
                throw TpAutoDataException("storage down")
            }
        }

    }

    @Test
    fun `hydrate restores the mode and allow-list`() {
        repository.snapshot = TpAutoSnapshot(
            modes = mapOf(owner to TpAutoMode.TRUSTED),
            allowed = mapOf(owner to setOf(alice)),
        )

        service.hydrate()

        assertEquals(TpAutoMode.TRUSTED, service.mode(owner))
        assertEquals(listOf(alice), service.allowedBy(owner))
    }

    @Test
    fun `an absent owner defaults to off`() {
        assertEquals(TpAutoMode.OFF, service.mode(owner))
    }

    @Test
    fun `only TO_TARGET requests are auto-accepted`() = runBlocking {
        service.setMode(owner, TpAutoMode.ALL)

        assertTrue(
            service.shouldAutoAccept(
                owner,
                alice,
                TeleportRequestType.TO_TARGET
            )
        )
        assertFalse(
            service.shouldAutoAccept(
                owner,
                alice,
                TeleportRequestType.BRING_TARGET
            )
        )
    }

    @Test
    fun `off never auto-accepts`() = runBlocking {
        service.setMode(owner, TpAutoMode.OFF)

        assertFalse(
            service.shouldAutoAccept(
                owner,
                alice,
                TeleportRequestType.TO_TARGET
            )
        )
    }

    @Test
    fun `all accepts every sender`() = runBlocking {
        service.setMode(owner, TpAutoMode.ALL)

        assertTrue(
            service.shouldAutoAccept(
                owner,
                alice,
                TeleportRequestType.TO_TARGET
            )
        )
        assertTrue(
            service.shouldAutoAccept(
                owner,
                bob,
                TeleportRequestType.TO_TARGET
            )
        )
    }

    @Test
    fun `trusted accepts only allow-listed senders`() = runBlocking {
        service.setMode(owner, TpAutoMode.TRUSTED)
        service.allow(owner, alice)

        assertTrue(
            service.shouldAutoAccept(
                owner,
                alice,
                TeleportRequestType.TO_TARGET
            )
        )
        assertFalse(
            service.shouldAutoAccept(
                owner,
                bob,
                TeleportRequestType.TO_TARGET
            )
        )
    }

    @Test
    fun `trusted with an empty allow-list accepts nobody`() = runBlocking {
        service.setMode(owner, TpAutoMode.TRUSTED)

        assertFalse(
            service.shouldAutoAccept(
                owner,
                alice,
                TeleportRequestType.TO_TARGET
            )
        )
    }

    @Test
    fun `allow-list cap is enforced`() = runBlocking {
        settings = settings.copy(maxAutoAllowedPlayers = 1)

        assertEquals(
            TpAutoAllowResult.Added,
            service.allow(owner, alice)
        )
        assertEquals(
            TpAutoAllowResult.LimitReached,
            service.allow(owner, bob)
        )
    }

    @Test
    fun `a player cannot allow themselves`() = runBlocking {
        assertEquals(
            TpAutoAllowResult.Self,
            service.allow(owner, owner)
        )
    }

    @Test
    fun `duplicate allow is reported and not written twice`() = runBlocking {
        service.allow(owner, alice)
        assertEquals(
            TpAutoAllowResult.AlreadyAllowed,
            service.allow(owner, alice)
        )
        assertEquals(
            1,
            repository.allowedWrites.size
        )
    }

    @Test
    fun `deny removes an entry and reports missing entries`() = runBlocking {
        service.setMode(owner, TpAutoMode.TRUSTED)
        service.allow(owner, alice)

        assertEquals(
            TpAutoDisallowResult.Removed,
            service.disallow(owner, alice)
        )
        assertFalse(
            service.shouldAutoAccept(
                owner,
                alice,
                TeleportRequestType.TO_TARGET
            )
        )
        assertEquals(
            TpAutoDisallowResult.NotAllowed,
            service.disallow(owner, alice)
        )
    }

    @Test
    fun `storage failures are reported without publishing state`() = runBlocking {
        repository.failing = true

        assertEquals(
            TpAutoToggleResult.StorageUnavailable,
            service.setMode(owner, TpAutoMode.ALL)
        )
        assertEquals(TpAutoMode.OFF, service.mode(owner))

        assertEquals(
            TpAutoAllowResult.StorageUnavailable,
            service.allow(owner, alice)
        )
        assertTrue(service.allowedBy(owner).isEmpty())
    }

}
