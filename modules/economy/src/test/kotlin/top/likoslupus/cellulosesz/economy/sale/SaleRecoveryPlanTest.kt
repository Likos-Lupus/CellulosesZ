package top.likoslupus.cellulosesz.economy.sale

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import java.util.*

/**
 * The crash-recovery decision. It must discard `PREPARED`, settle committed `TAKEN`, restore
 * uncommitted `TAKEN`, and never act on a terminal row.
 */
class SaleRecoveryPlanTest {

    private fun record(
        state: SaleIntentState,
        saleId: UUID = UUID.randomUUID(),
        name: String = "Alex",
    ) =
        SaleIntentRecord(
            saleId = saleId,
            playerId = UUID.randomUUID(),
            playerName = name,
            state = state,
            itemId = "minecraft:diamond",
            itemCount = 3,
            totalMinor = 15000L,
            payload = "[]",
            transactionId = null,
            createdAtMs = 1L,
            updatedAtMs = 1L,
        )

    @Test
    fun `prepared rows are discarded`() {
        val prepared = record(SaleIntentState.PREPARED)
        val actions = SaleRecoveryPlan.decide(listOf(prepared)) { null }

        assertEquals(
            listOf(SaleRecoveryAction.MarkRolledBack(prepared)),
            actions,
        )
    }

    @Test
    fun `taken rows without a ledger credit are restored`() {
        val taken = record(SaleIntentState.TAKEN)
        val actions = SaleRecoveryPlan.decide(listOf(taken)) { null }

        assertEquals(
            listOf(SaleRecoveryAction.Restore(taken)),
            actions,
        )
    }

    @Test
    fun `taken rows with a ledger credit are only stamped committed`() {
        val taken = record(SaleIntentState.TAKEN)
        val transactionId = UUID.randomUUID()
        val actions = SaleRecoveryPlan.decide(listOf(taken)) { transactionId }

        assertEquals(
            listOf(SaleRecoveryAction.MarkCommitted(taken, transactionId)),
            actions,
        )
    }

    @Test
    fun `terminal rows produce no action`() {
        val actions = SaleRecoveryPlan.decide(
            listOf(
                record(SaleIntentState.COMMITTED),
                record(SaleIntentState.ROLLED_BACK),
            )
        ) { UUID.randomUUID() }

        assertTrue(actions.isEmpty())
    }

    @Test
    fun `the ledger lookup is keyed by the sale id`() {
        val taken = record(SaleIntentState.TAKEN)
        var seen: String? = null
        SaleRecoveryPlan.decide(listOf(taken)) { key ->
            seen = key
            null
        }
        assertEquals(taken.saleId.toString(), seen)
    }

}
