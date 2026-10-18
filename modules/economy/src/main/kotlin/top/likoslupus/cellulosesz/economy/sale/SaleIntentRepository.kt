package top.likoslupus.cellulosesz.economy.sale

import java.time.Instant
import java.util.*

/**
 * The single owner of `cz_economy_sale_intents`. Each transition is a guarded conditional update so
 * a duplicate or out-of-order transition can never regress a terminal state. The payload is opaque
 * to this layer.
 */
internal interface SaleIntentRepository {

    /** Persists a fresh `PREPARED` intent before any item is removed. */
    suspend fun insert(record: SaleIntentRecord)

    /** `PREPARED -> TAKEN`; false when the row was not in `PREPARED`. */
    suspend fun markTaken(saleId: UUID, at: Instant): Boolean

    /** `TAKEN -> COMMITTED`; records the ledger transaction id. */
    suspend fun markCommitted(
        saleId: UUID,
        transactionId: UUID,
        at: Instant
    ): Boolean

    /** `PREPARED|TAKEN -> ROLLED_BACK`; the terminal tombstone. */
    suspend fun markRolledBack(saleId: UUID, at: Instant): Boolean

    /** Every intent that is neither `COMMITTED` nor `ROLLED_BACK`, oldest first. */
    suspend fun loadOpen(): List<SaleIntentRecord>

}
