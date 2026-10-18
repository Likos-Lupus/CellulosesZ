package top.likoslupus.cellulosesz.player.nickname

import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test
import top.likoslupus.cellulosesz.core.player.KnownPlayerIdentity
import top.likoslupus.cellulosesz.core.player.KnownPlayerResolver
import top.likoslupus.cellulosesz.core.player.identity.PersistedPlayerIdentity
import top.likoslupus.cellulosesz.core.player.identity.PlayerIdentityRepository
import top.likoslupus.cellulosesz.core.runtime.ServerThreadRunner
import top.likoslupus.cellulosesz.player.config.NicknameSettings
import java.time.Clock
import java.time.Instant
import java.time.ZoneOffset
import java.util.*
import kotlinx.coroutines.runBlocking

class NicknameServiceTest {

    private val alice = UUID.randomUUID()
    private val bob = UUID.randomUUID()

    private val settings = NicknameSettings(
        enabled = true,
        minimumVisibleLength = 2,
        maximumVisibleLength = 12,
        allowSpaces = true,
    )

    private val known = FakeKnownResolver()
    private val identities = FakeIdentityRepository()
    private val repository = FakeNicknameRepository()
    private val service = NicknameService(
        runner = ImmediateRunner,
        known = known,
        identities = identities,
        repository = repository,
        settings = { settings },
        clock = Clock.fixed(
            Instant.parse("2026-01-01T00:00:00Z"),
            ZoneOffset.UTC
        ),
    )

    @Test
    fun `stores a valid nickname`() =
        runBlocking {
            val result = service.set(alice, "Cool Name")

            assertEquals(
                "Cool Name",
                (result as NicknameSetResult.Set).display
            )
            assertEquals(
                "cool name",
                repository.byPlayer[alice]?.normalized
            )
        }

    @Test
    fun `keeps colour codes in the display and strips them from the normalized form`() =
        runBlocking {
            val colored = serviceWith(settings.copy(allowColors = true))

            val result = colored.set(alice, "&aCool", colorsAllowed = true)

            assertEquals("&aCool", (result as NicknameSetResult.Set).display)
            assertEquals("&aCool", repository.byPlayer[alice]?.display)
            assertEquals("cool", repository.byPlayer[alice]?.normalized)
        }

    @Test
    fun `refuses colour codes when the config does not allow them`() =
        runBlocking {
            assertEquals(
                NicknameSetResult.ColorsNotAllowed,
                service.set(
                    alice,
                    "&aCool",
                    colorsAllowed = true
                )
            )
        }

    @Test
    fun `reset on name change discards a nickname set under the old name`() =
        runBlocking {
            val resetting = serviceWith(settings.copy(resetOnNameChange = true))
            repository.save(
                StoredNickname(
                    playerId = alice,
                    display = "Cool",
                    normalized = "cool",
                    ownerName = "OldName",
                    updatedAt = Instant.EPOCH,
                )
            )

            resetting.loadDisplay(alice, "NewName")

            assertNull(repository.byPlayer[alice])
        }

    @Test
    fun `reports length and character failures`() =
        runBlocking {
            assertInstanceOf(
                NicknameSetResult.TooShort::class.java,
                service.set(alice, "a")
            )
            assertInstanceOf(
                NicknameSetResult.TooLong::class.java,
                service.set(alice, "x".repeat(13))
            )
            assertEquals(
                NicknameSetResult.InvalidCharacters,
                service.set(alice, "bad!")
            )
            assertEquals(
                NicknameSetResult.Empty,
                service.set(alice, "   ")
            )
        }

    @Test
    fun `rejects a nickname owned by another player`() =
        runBlocking {
            repository.save(
                stored(
                    bob,
                    "Taken",
                    "taken"
                )
            )

            assertEquals(
                NicknameSetResult.Taken,
                service.set(alice, "taken")
            )
        }

    @Test
    fun `rejects a nickname that matches another player's name`() =
        runBlocking {
            known.record(KnownPlayerIdentity(bob, "Bob"))

            assertEquals(
                NicknameSetResult.Reserved,
                service.set(alice, "bob")
            )
        }

    @Test
    fun `keeps your own nickname when the normalized form is unchanged`() =
        runBlocking {
            repository.save(
                stored(
                    alice,
                    "Cool",
                    "cool"
                )
            )

            assertEquals(
                NicknameSetResult.Set("COOL"),
                service.set(alice, "COOL")
            )
            assertEquals(
                "COOL",
                repository.byPlayer[alice]?.display
            )
        }

    @Test
    fun `rejects when disabled`() =
        runBlocking {
            val disabled = serviceWith(settings.copy(enabled = false))

            assertEquals(
                NicknameSetResult.Disabled,
                disabled.set(alice, "Cool")
            )
        }

    @Test
    fun `resolves a nickname to the account name`() =
        runBlocking {
            repository.save(
                stored(
                    alice,
                    "Cool",
                    "cool"
                )
            )
            known.record(
                KnownPlayerIdentity(
                    alice,
                    "Alice"
                )
            )

            val result = service.resolve("  COOL ")

            assertEquals(
                alice,
                (result as NicknameLookup.Found).playerId
            )
            assertEquals(
                "Alice",
                result.actualName
            )
        }

    @Test
    fun `falls back to the durable identity when the player is not indexed`() =
        runBlocking {
            repository.save(
                stored(
                    alice,
                    "Cool",
                    "cool"
                )
            )
            identities.byId[alice] = PersistedPlayerIdentity(
                id = alice,
                name = "Alice",
                firstSeen = Instant.EPOCH,
                lastSeen = Instant.EPOCH,
            )

            val result = service.resolve("cool")

            assertEquals(
                "Alice",
                (result as NicknameLookup.Found).actualName
            )
        }

    @Test
    fun `reports an unknown nickname`() =
        runBlocking {
            assertEquals(
                NicknameLookup.NotFound,
                service.resolve("ghost")
            )
        }

    @Test
    fun `clears a nickname`() =
        runBlocking {
            repository.save(
                stored(
                    alice,
                    "Cool",
                    "cool"
                )
            )

            assertEquals(
                NicknameSetResult.Cleared,
                service.clear(alice)
            )
            assertNull(repository.byPlayer[alice])
        }

    @Test
    fun `staff can set another player's nickname and records the actor`() =
        runBlocking {
            known.record(KnownPlayerIdentity(bob, "Bob"))

            val result = service.setFor(alice, "Bob", "Cool")

            assertEquals(
                "Cool",
                (repository.byPlayer[bob]?.display)
            )
            assertEquals(alice, repository.byPlayer[bob]?.updatedBy)
            assertEquals("Bob", (result as NicknameForResult.Applied).targetName)
        }

    @Test
    fun `staff set resolves an offline player through the identity index`() =
        runBlocking {
            val carol = UUID.randomUUID()
            identities.byId[carol] = PersistedPlayerIdentity(
                id = carol,
                name = "Carol",
                firstSeen = Instant.EPOCH,
                lastSeen = Instant.EPOCH,
            )

            val result = service.setFor(null, "Carol", "Cool")

            assertEquals(
                "Cool",
                repository.byPlayer[carol]?.display
            )
            assertEquals("Carol", (result as NicknameForResult.Applied).targetName)
        }

    @Test
    fun `staff set reports an unknown target`() =
        runBlocking {
            assertEquals(
                NicknameForResult.TargetNotFound,
                service.setFor(alice, "Ghost", "Cool"),
            )
        }

    private fun serviceWith(next: NicknameSettings): NicknameService =
        NicknameService(
            runner = ImmediateRunner,
            known = known,
            identities = identities,
            repository = repository,
            settings = { next },
            clock = Clock.systemUTC(),
        )

    private fun stored(
        id: UUID,
        display: String,
        normalized: String
    ): StoredNickname =
        StoredNickname(
            playerId = id,
            display = display,
            normalized = normalized,
            updatedAt = Instant.EPOCH,
        )

    private object ImmediateRunner : ServerThreadRunner {

        override suspend fun <T> run(block: () -> T): T =
            block()

    }

    private class FakeNicknameRepository : NicknameRepository {

        val byPlayer: MutableMap<UUID, StoredNickname> = mutableMapOf()

        override suspend fun find(playerId: UUID): StoredNickname? =
            byPlayer[playerId]

        override suspend fun findByNormalized(normalized: String): StoredNickname? =
            byPlayer.values.firstOrNull { it.normalized == normalized }

        override suspend fun save(record: StoredNickname) {
            byPlayer[record.playerId] = record
        }

        override suspend fun delete(playerId: UUID): Boolean =
            byPlayer.remove(playerId) != null

    }

    private class FakeKnownResolver : KnownPlayerResolver {

        private val byId = mutableMapOf<UUID, String>()

        fun record(identity: KnownPlayerIdentity) {
            byId[identity.id] = identity.name
        }

        override fun onlineByName(name: String): KnownPlayerIdentity? =
            null

        override fun onlineById(id: UUID): KnownPlayerIdentity? =
            null

        override fun knownByName(name: String): KnownPlayerIdentity? =
            byId.entries
                    .firstOrNull { it.value.equals(name, ignoreCase = true) }
                    ?.let { KnownPlayerIdentity(it.key, it.value) }

        override fun knownById(id: UUID): KnownPlayerIdentity? =
            byId[id]?.let { KnownPlayerIdentity(id, it) }

    }

    private class FakeIdentityRepository : PlayerIdentityRepository {

        val byId = mutableMapOf<UUID, PersistedPlayerIdentity>()

        override suspend fun loadAll(): List<PersistedPlayerIdentity> =
            byId.values.toList()

        override suspend fun findById(id: UUID): PersistedPlayerIdentity? =
            byId[id]

        override suspend fun findCandidatesByName(name: String): List<PersistedPlayerIdentity> =
            byId.values.filter { it.name.equals(name, ignoreCase = true) }

        override suspend fun observe(identity: KnownPlayerIdentity, at: Instant) {
            byId[identity.id] = PersistedPlayerIdentity(
                id = identity.id,
                name = identity.name,
                firstSeen = at,
                lastSeen = at,
            )
        }

    }

}
