package top.likoslupus.cellulosesz.utility.kit

import kotlinx.coroutines.runBlocking
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test
import top.likoslupus.cellulosesz.core.player.KnownPlayerIdentity
import java.nio.file.Files
import java.nio.file.Path
import java.time.Instant
import java.util.*

class FileKitClaimRepositoryTest {

    private val root: Path = Files.createTempDirectory("cellulosesz-kit-claims")
    private val repository = FileKitClaimRepository { root }
    private val player = KnownPlayerIdentity(UUID.randomUUID(), "Alice")
    private val starter = KitName.parse("starter")!!
    private val kit = KitDefinition(
        id = KitId(UUID.randomUUID()),
        name = starter,
        reuse = KitReusePolicy.Once,
        items = emptyList(),
    )
    private val now = Instant.parse("2026-01-01T00:00:00Z")

    @Test
    fun `missing file loads empty`() = runBlocking {
        assertTrue(repository.load(player.id).isEmpty())
    }

    @Test
    fun `reserve then load reports reserved`() = runBlocking {
        repository.reserve(player, kit, now)

        val claim = repository.load(player.id).getValue(starter)
        assertEquals(kit.id.value, claim.kitId)
        assertEquals(KitClaimStatus.RESERVED, claim.status)
        assertEquals(now.toEpochMilli(), claim.claimedAt.toEpochMilli())
    }

    @Test
    fun `mark delivered transitions status`() = runBlocking {
        repository.reserve(player, kit, now)
        repository.markDelivered(player, kit, now.plusSeconds(5))

        assertEquals(
            KitClaimStatus.DELIVERED,
            repository.load(player.id).getValue(starter).status
        )
    }

    @Test
    fun `reset removes a single claim`() = runBlocking {
        repository.reserve(player, kit, now)

        assertTrue(repository.reset(player.id, starter))
        assertTrue(repository.load(player.id).isEmpty())
        assertFalse(repository.reset(player.id, starter))
    }

    @Test
    fun `corrupt file is not overwritten`() = runBlocking {
        val file = root.resolve("kit-claims").resolve("${player.id}.json")
        Files.createDirectories(file.parent)
        Files.writeString(file, "{ not valid json")

        assertThrows(KitClaimDataException::class.java) {
            runBlocking { repository.load(player.id) }
        }
        assertTrue(Files.exists(file))
    }

    @Test
    fun `future schema is rejected`() = runBlocking {
        val file = root.resolve("kit-claims").resolve("${player.id}.json")
        Files.createDirectories(file.parent)
        Files.writeString(
            file,
            /* language=JSON */
            """
            {
              "schemaVersion": 99,
              "playerId": "${player.id}",
              "playerName": "Alice",
              "claims": {}
            }
            """.trimIndent(),
        )

        assertThrows(KitClaimDataException::class.java) {
            runBlocking { repository.load(player.id) }
        }
        Unit
    }

    @Test
    fun `filename mismatch is rejected`() = runBlocking {
        val other = KnownPlayerIdentity(UUID.randomUUID(), "Bob")
        repository.reserve(other, kit, now)

        val source = root
                .resolve("kit-claims")
                .resolve("${other.id}.json")
        val target = root
                .resolve("kit-claims")
                .resolve("${player.id}.json")
        Files.copy(source, target)

        assertThrows(KitClaimDataException::class.java) {
            runBlocking { repository.load(player.id) }
        }
        Unit
    }

}
