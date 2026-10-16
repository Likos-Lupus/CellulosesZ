package top.likoslupus.cellulosesz.utility.kit

import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test
import top.likoslupus.cellulosesz.core.player.KnownPlayerIdentity
import top.likoslupus.cellulosesz.foundation.persistence.KeyedMutex
import top.likoslupus.cellulosesz.utility.*
import top.likoslupus.cellulosesz.utility.config.KitSettings
import java.time.Instant
import java.util.*
import kotlinx.coroutines.runBlocking

class KitGrantServiceTest {

    private val alice = KnownPlayerIdentity(UUID.randomUUID(), "Alice")
    private val known = FakeKnownPlayerResolver().apply { add(alice) }
    private val definitions = FakeKitRepository()
    private val claims = FakeKitClaimRepository()
    private val grants = FakeKitGrantRepository()
    private val inventory = FakeKitInventoryBackend()
    private val clock = MutableClock(Instant.parse("2026-01-01T00:00:00Z"))

    private val addonId = "example_quests"
    private val grantId = "reward-1"

    private fun service(settings: KitSettings = KitSettings()): Pair<KitService, KitGrantService> {
        val kits = KitService(
            runner = ImmediateServerThreadRunner,
            definitions = definitions,
            claims = claims,
            inventory = inventory,
            known = known,
            claimLocks = KeyedMutex(),
            settings = { settings },
            clock = clock,
        )
        val grantService = KitGrantService(
            runner = ImmediateServerThreadRunner,
            kits = kits,
            grants = grants,
            inventory = inventory,
            known = known,
            settings = { settings },
            clock = clock,
        )
        return kits to grantService
    }

    private suspend fun seed(kits: KitService, name: String = "starter") {
        definitions.definitions[KitName.parse(name)!!] = KitDefinition(
            id = KitId(UUID.randomUUID()),
            name = KitName.parse(name)!!,
            reuse = KitReusePolicy.Once,
            items = emptyList(),
        )
        kits.load()
    }

    @Test
    fun `grant delivers and records delivered`() =
        runBlocking {
            val (kits, service) = service()
            seed(kits)

            val result = service.grant(
                addonId,
                alice.id,
                "starter",
                grantId
            )

            assertTrue(result is GrantKitResult.Delivered)
            assertEquals(
                KitGrantStatus.DELIVERED,
                grants.stateOf(addonId, grantId, alice.id)
            )
        }

    @Test
    fun `duplicate delivered grant is already processed`() =
        runBlocking {
            val (kits, service) = service()
            seed(kits)

            assertTrue(
                service.grant(
                    addonId,
                    alice.id,
                    "starter",
                    grantId
                ) is GrantKitResult.Delivered
            )
            assertEquals(
                GrantKitResult.AlreadyProcessed,
                service.grant(
                    addonId,
                    alice.id,
                    "starter",
                    grantId
                ),
            )
            assertEquals(1, inventory.deliverCalls)
        }

    @Test
    fun `reserved grant is previously uncertain and never redelivers`() =
        runBlocking {
            val (kits, service) = service()
            seed(kits)
            inventory.deliveryResult = KitDeliveryResult.PlayerOffline

            assertEquals(
                GrantKitResult.DeliveryFailed,
                service.grant(
                    addonId,
                    alice.id,
                    "starter",
                    grantId
                ),
            )
            assertEquals(
                KitGrantStatus.RESERVED,
                grants.stateOf(addonId, grantId, alice.id)
            )
            assertEquals(
                GrantKitResult.PreviouslyUncertain,
                service.grant(
                    addonId,
                    alice.id,
                    "starter",
                    grantId
                ),
            )
            assertEquals(1, inventory.deliverCalls)
        }

    @Test
    fun `grant never consumes the player's own claim opportunity`() =
        runBlocking {
            val (kits, service) = service()
            seed(kits)

            assertTrue(
                service.grant(
                    addonId,
                    alice.id,
                    "starter",
                    grantId
                ) is GrantKitResult.Delivered
            )
            assertTrue(
                kits.claim(
                    alice.id,
                    "starter"
                ) is ClaimKitResult.Delivered
            )
        }

    @Test
    fun `full inventory rejects without reserving`() =
        runBlocking {
            val (kits, service) = service()
            seed(kits)
            inventory.preflightResult = KitPreflightResult.Full

            assertEquals(
                GrantKitResult.InventoryFull,
                service.grant(
                    addonId,
                    alice.id,
                    "starter",
                    grantId
                ),
            )
            assertNull(
                grants.stateOf(
                    addonId,
                    grantId,
                    alice.id
                )
            )
            assertEquals(0, inventory.deliverCalls)
        }

    @Test
    fun `unknown kit is not found`() =
        runBlocking {
            val (kits, service) = service()
            seed(kits)

            assertEquals(
                GrantKitResult.KitNotFound,
                service.grant(
                    addonId,
                    alice.id,
                    "missing",
                    grantId
                ),
            )
        }

    @Test
    fun `offline player is player offline`() =
        runBlocking {
            val (kits, service) = service()
            seed(kits)

            assertEquals(
                GrantKitResult.PlayerOffline,
                service.grant(
                    addonId,
                    UUID.randomUUID(),
                    "starter",
                    grantId
                ),
            )
        }

    @Test
    fun `disabled feature is disabled`() =
        runBlocking {
            val (kits, service) = service(KitSettings(enabled = false))
            seed(kits)

            assertEquals(
                GrantKitResult.Disabled,
                service.grant(
                    addonId,
                    alice.id,
                    "starter",
                    grantId
                ),
            )
        }

}
