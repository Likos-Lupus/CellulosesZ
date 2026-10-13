package top.likoslupus.cellulosesz.utility.item

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import top.likoslupus.cellulosesz.core.player.KnownPlayerIdentity
import top.likoslupus.cellulosesz.utility.FakeItemUtilityBackend
import top.likoslupus.cellulosesz.utility.FakeKnownPlayerResolver
import top.likoslupus.cellulosesz.utility.config.ItemEditingSettings
import top.likoslupus.cellulosesz.utility.config.ItemUtilitySettings
import java.util.*

class ItemUtilityServiceTest {

    private val alice = KnownPlayerIdentity(UUID.randomUUID(), "Alice")
    private val known = FakeKnownPlayerResolver().apply { add(alice) }
    private val backend = FakeItemUtilityBackend()

    private fun service(
        settings: ItemUtilitySettings = ItemUtilitySettings(),
        editing: ItemEditingSettings = ItemEditingSettings(clearConfirmationSeconds = 15),
    ): ItemUtilityService =
        ItemUtilityService(
            backend = backend,
            known = known,
            settings = { settings },
            editing = { editing },
            clock = { java.time.Instant.parse("2026-01-01T00:00:00Z") },
            confirmationCode = { "123456" },
        )

    @Test
    fun `repair is reported for the resolved player`() {
        backend.repairResult = RepairBackendResult.Repaired(3, 1)

        val result = service().repair(alice.id, RepairScope.ALL)

        assertEquals(
            RepairResult.Repaired(
                playerName = "Alice",
                count = 3,
                skippedEnchanted = 1
            ),
            result
        )
    }

    @Test
    fun `repair is disabled by config`() {
        val result = service(ItemUtilitySettings(repairEnabled = false))
                .repair(
                    alice.id,
                    RepairScope.HAND
                )

        assertEquals(RepairResult.Disabled, result)
    }

    @Test
    fun `repair reports an offline target`() {
        val result = service()
                .repair(
                    UUID.randomUUID(),
                    RepairScope.HAND
                )

        assertEquals(
            RepairResult.TargetOffline,
            result
        )
    }

    @Test
    fun `more is disabled by default`() {
        assertEquals(
            MoreResult.Disabled,
            service().more(alice.id, null)
        )
    }

    @Test
    fun `more reports a filled stack when enabled`() {
        backend.moreResult = MoreBackendResult.Filled("Diamond Sword")

        val result = service(ItemUtilitySettings(moreEnabled = true))
                .more(
                    alice.id,
                    16
                )

        assertEquals(
            MoreResult.Filled(
                playerName = "Alice",
                itemName = "Diamond Sword"
            ),
            result
        )
    }

    @Test
    fun `condense is disabled by default`() {
        assertEquals(
            CondenseResult.Disabled,
            service().condense(alice.id)
        )
    }

    @Test
    fun `condense reports conversions when enabled`() {
        backend.condenseResult = CondenseBackendResult.Condensed(2)

        val result = service(ItemUtilitySettings(condenseEnabled = true))
                .condense(alice.id)

        assertTrue(result is CondenseResult.Condensed)
        assertEquals(
            2,
            (result as CondenseResult.Condensed).conversions
        )
    }

    private fun snapshotOf(digest: String): InventorySnapshot =
        InventorySnapshot(
            scope = ClearScope.MAIN,
            stacks = 1,
            fingerprints = listOf(SlotFingerprint("main:0", digest)),
        )

    @Test
    fun `clear preview issues a code and confirm applies when unchanged`() {
        backend.snapshotResult = InventorySnapshotResult.Snapshot(snapshotOf("a"))
        backend.clearResult = ClearInventoryBackendResult.Cleared(1)

        val service = service()
        val request = service.requestInventoryClear(alice.id, ClearScope.MAIN)

        assertTrue(request is ClearRequestResult.Issued)
        assertEquals("123456", (request as ClearRequestResult.Issued).code)
        assertEquals(1, request.stacks)

        val confirm = service.confirmInventoryClear(alice.id, "123456")

        assertEquals(ClearInventoryResult.Cleared("Alice", 1, null), confirm)
    }

    @Test
    fun `clear confirm is cancelled when the inventory changed`() {
        backend.snapshotResult = InventorySnapshotResult.Snapshot(snapshotOf("a"))
        val service = service()
        service.requestInventoryClear(alice.id, ClearScope.MAIN)

        backend.snapshotResult = InventorySnapshotResult.Snapshot(snapshotOf("b"))

        assertEquals(
            ClearInventoryResult.InventoryChanged,
            service.confirmInventoryClear(alice.id, "123456"),
        )
    }

    @Test
    fun `clear confirm rejects a wrong code`() {
        backend.snapshotResult = InventorySnapshotResult.Snapshot(snapshotOf("a"))
        val service = service()
        service.requestInventoryClear(alice.id, ClearScope.MAIN)

        assertEquals(
            ClearInventoryResult.InvalidCode,
            service.confirmInventoryClear(alice.id, "000000"),
        )
    }

    @Test
    fun `clear preview reports an offline target`() {
        assertEquals(
            ClearRequestResult.TargetOffline,
            service().requestInventoryClear(UUID.randomUUID(), ClearScope.MAIN),
        )
    }

}
