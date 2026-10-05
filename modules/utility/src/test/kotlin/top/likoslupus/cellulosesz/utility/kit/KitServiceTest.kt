package top.likoslupus.cellulosesz.utility.kit

import kotlinx.coroutines.runBlocking
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test
import top.likoslupus.cellulosesz.core.player.KnownPlayerIdentity
import top.likoslupus.cellulosesz.foundation.persistence.KeyedMutex
import top.likoslupus.cellulosesz.utility.*
import top.likoslupus.cellulosesz.utility.config.KitSettings
import java.time.Duration
import java.time.Instant
import java.util.*

class KitServiceTest {

    private val alice = KnownPlayerIdentity(UUID.randomUUID(), "Alice")
    private val known = FakeKnownPlayerResolver().apply { add(alice) }
    private val repository = FakeKitRepository()
    private val claims = FakeKitClaimRepository()
    private val inventory = FakeKitInventoryBackend()
    private val clock = MutableClock(Instant.parse("2026-01-01T00:00:00Z"))

    private fun service(settings: KitSettings = KitSettings()): KitService =
        KitService(
            runner = ImmediateServerThreadRunner,
            definitions = repository,
            claims = claims,
            inventory = inventory,
            known = known,
            claimLocks = KeyedMutex(),
            settings = { settings },
            clock = clock,
        )

    private suspend fun seed(
        service: KitService,
        name: String = "starter",
        reuse: KitReusePolicy,
        id: UUID = UUID.randomUUID(),
    ) {
        val definition = KitDefinition(
            id = KitId(id),
            name = KitName.parse(name)!!,
            reuse = reuse,
            items = emptyList(),
        )
        repository.definitions[definition.name] = definition
        service.load()
    }

    private val starter = KitName.parse("starter")!!

    @Test
    fun `always kit delivers without durable claim`() =
        runBlocking {
            val service = service()
            seed(service, reuse = KitReusePolicy.Always)

            val result = service.claim(alice.id, "starter")

            assertTrue(result is ClaimKitResult.Delivered)
            assertNull(claims.statusOf(alice.id, starter))
        }

    @Test
    fun `once kit blocks the second claim`() =
        runBlocking {
            val service = service()
            seed(service, reuse = KitReusePolicy.Once)

            assertTrue(
                service.claim(
                    alice.id,
                    "starter"
                ) is ClaimKitResult.Delivered
            )
            assertEquals(
                ClaimKitResult.AlreadyUsed,
                service.claim(alice.id, "starter")
            )
            assertEquals(1, inventory.deliverCalls)
        }

    @Test
    fun `cooldown kit blocks until elapsed`() =
        runBlocking {
            val service = service()
            seed(
                service,
                reuse = KitReusePolicy.Cooldown(Duration.ofHours(1))
            )

            assertTrue(
                service.claim(
                    alice.id,
                    "starter"
                ) is ClaimKitResult.Delivered
            )

            val blocked = service.claim(alice.id, "starter")
            assertTrue(blocked is ClaimKitResult.Cooldown)
            assertTrue((blocked as ClaimKitResult.Cooldown).remaining.seconds > 0)

            clock.advance(Duration.ofHours(1))
            assertTrue(
                service.claim(
                    alice.id,
                    "starter"
                ) is ClaimKitResult.Delivered
            )
        }

    @Test
    fun `full inventory rejects without reserving`() =
        runBlocking {
            val service = service()
            seed(service, reuse = KitReusePolicy.Once)
            inventory.preflightResult = KitPreflightResult.Full

            assertEquals(
                ClaimKitResult.InventoryFull,
                service.claim(alice.id, "starter")
            )
            assertNull(claims.statusOf(alice.id, starter))
            assertEquals(0, inventory.deliverCalls)
        }

    @Test
    fun `reservation failure blocks delivery`() =
        runBlocking {
            val service = service()
            seed(service, reuse = KitReusePolicy.Once)
            claims.failReserve = true

            assertEquals(
                ClaimKitResult.StorageUnavailable,
                service.claim(alice.id, "starter")
            )
            assertEquals(0, inventory.deliverCalls)
        }

    @Test
    fun `delivery failure after reservation keeps the claim reserved`() =
        runBlocking {
            val service = service()
            seed(service, reuse = KitReusePolicy.Once)
            inventory.deliveryResult = KitDeliveryResult.PlayerOffline

            assertEquals(
                ClaimKitResult.DeliveryFailedAfterReservation,
                service.claim(alice.id, "starter"),
            )
            assertEquals(
                KitClaimStatus.RESERVED,
                claims.statusOf(alice.id, starter)
            )
            assertEquals(
                ClaimKitResult.AlreadyUsed,
                service.claim(alice.id, "starter")
            )
        }

    @Test
    fun `finalization failure still counts as delivered but blocks retry`() =
        runBlocking {
            val service = service()
            seed(service, reuse = KitReusePolicy.Once)
            inventory.deliveryResult = KitDeliveryResult.Delivered(2)
            claims.failFinalize = true

            val result = service.claim(alice.id, "starter")
            assertTrue(result is ClaimKitResult.Delivered)
            result as ClaimKitResult.Delivered
            assertEquals(2, result.droppedStacks)
            assertTrue(!result.claimRecordedFully)
            assertEquals(
                KitClaimStatus.RESERVED,
                claims.statusOf(alice.id, starter)
            )
            assertEquals(
                ClaimKitResult.AlreadyUsed,
                service.claim(alice.id, "starter")
            )
            assertEquals(
                1,
                inventory.deliverCalls
            )
        }

    @Test
    fun `delete and recreate resets the claim identity`() =
        runBlocking {
            val service = service()
            seed(service, reuse = KitReusePolicy.Once, id = UUID.randomUUID())
            assertTrue(
                service.claim(
                    alice.id,
                    "starter"
                ) is ClaimKitResult.Delivered
            )

            assertTrue(service.delete("starter") is DeleteKitResult.Deleted)
            seed(service, reuse = KitReusePolicy.Once, id = UUID.randomUUID())

            assertTrue(
                service.claim(
                    alice.id,
                    "starter"
                ) is ClaimKitResult.Delivered
            )
        }

}
