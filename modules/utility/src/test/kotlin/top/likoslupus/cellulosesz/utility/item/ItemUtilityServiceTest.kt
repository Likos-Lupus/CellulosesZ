package top.likoslupus.cellulosesz.utility.item

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import top.likoslupus.cellulosesz.core.player.KnownPlayerIdentity
import top.likoslupus.cellulosesz.utility.FakeItemUtilityBackend
import top.likoslupus.cellulosesz.utility.FakeKnownPlayerResolver
import top.likoslupus.cellulosesz.utility.config.ItemUtilitySettings
import java.util.*

class ItemUtilityServiceTest {

    private val alice = KnownPlayerIdentity(UUID.randomUUID(), "Alice")
    private val known = FakeKnownPlayerResolver().apply { add(alice) }
    private val backend = FakeItemUtilityBackend()

    private fun service(
        settings: ItemUtilitySettings = ItemUtilitySettings()
    ): ItemUtilityService =
        ItemUtilityService(
            backend = backend,
            known = known,
            settings = { settings }
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

}
