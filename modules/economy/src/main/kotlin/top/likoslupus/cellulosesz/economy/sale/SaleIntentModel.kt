package top.likoslupus.cellulosesz.economy.sale

import java.util.*

/** The ledger origin of every sale credit; the request key is the sale id. */
internal const val SELL_ORIGIN: String = "sell"

/** The reason code recorded on a sale ledger transaction. */
internal const val SELL_REASON: String = "item.sell"

/**
 * The durable lifecycle of one sale.
 *
 * `PREPARED` is written before the items leave the inventory; `TAKEN` proves the items are gone;
 * `COMMITTED` proves the credit landed; `ROLLED_BACK` is a terminal tombstone. The only crash window
 * that can lose value is `PREPARED` with the items already removed (recovery discards it) — no state
 * ever lets recovery mint money.
 */
internal enum class SaleIntentState {

    PREPARED,
    TAKEN,
    COMMITTED,
    ROLLED_BACK,

}

/** One persisted sale intent. [payload] holds the exact removed stacks (see [SaleItemCodec]). */
internal data class SaleIntentRecord(
    val saleId: UUID,
    val playerId: UUID,
    val playerName: String,
    val state: SaleIntentState,
    val itemId: String,
    val itemCount: Int,
    val totalMinor: Long,
    val payload: String,
    val transactionId: UUID?,
    val createdAtMs: Long,
    val updatedAtMs: Long,
)

/** What startup recovery must do with one open intent. */
internal sealed interface SaleRecoveryAction {

    /** No items were taken (or the ledger already paid); stamp a terminal state. */
    data class MarkRolledBack(val record: SaleIntentRecord) : SaleRecoveryAction

    /** The items are gone and the credit already committed; just record the transaction id. */
    data class MarkCommitted(
        val record: SaleIntentRecord,
        val transactionId: UUID
    ) : SaleRecoveryAction

    /** The items are gone but the credit never landed; the exact stacks must be handed back. */
    data class Restore(val record: SaleIntentRecord) : SaleRecoveryAction

}

/**
 * The pure recovery policy. Given the still-open intents and a lookup of the committed ledger
 * transaction per sale key, it decides every action without touching storage, inventory or the
 * clock. `COMMITTED` and `ROLLED_BACK` rows are never returned by the repository, but a record in a
 * terminal state is treated as a no-op here as well.
 */
internal object SaleRecoveryPlan {

    fun decide(
        open: List<SaleIntentRecord>,
        ledgerTransaction: (String) -> UUID?,
    ): List<SaleRecoveryAction> =
        open.mapNotNull { record ->
            when (record.state) {
                SaleIntentState.COMMITTED,
                SaleIntentState.ROLLED_BACK ->
                    null

                SaleIntentState.PREPARED ->
                    SaleRecoveryAction.MarkRolledBack(record)

                SaleIntentState.TAKEN ->
                    when (
                        val existing = ledgerTransaction(record.saleId.toString())
                    ) {
                        null -> SaleRecoveryAction.Restore(record)
                        else -> SaleRecoveryAction.MarkCommitted(record, existing)
                    }
            }
        }

}
