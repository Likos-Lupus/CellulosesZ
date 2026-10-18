package top.likoslupus.cellulosesz.economy.sale

import top.likoslupus.cellulosesz.core.runtime.RuntimeKernel
import top.likoslupus.cellulosesz.economy.ledger.EconomyDataException
import top.likoslupus.cellulosesz.economy.ledger.EconomyRepository
import java.time.Instant
import java.util.*
import java.util.concurrent.ConcurrentHashMap
import kotlin.time.Duration
import kotlin.time.Duration.Companion.seconds
import kotlinx.coroutines.delay

/**
 * Startup recovery and owed-stack restore for durable sale intents.
 *
 * The invariant is one-directional: **the only crash window can lose items, never create money.**
 * - `PREPARED` (written before the items left, possibly after they did) is discarded.
 * - `TAKEN` whose ledger credit landed is only stamped `COMMITTED`.
 * - `TAKEN` whose credit never landed hands the exact stacks back.
 * Recovery never writes a balance; the credit is the ledger's job and an already committed
 * transaction is detected, never repeated.
 *
 * A restore needs the owner online. Records parked because the owner was offline are retried by a
 * single long-lived coroutine for the life of the server session, so a crash-recovered stack returns
 * as soon as its owner next logs in.
 */
internal class SaleRecoveryService(
    private val kernel: RuntimeKernel,
    private val repository: SaleIntentRepository,
    private val ledger: EconomyRepository,
    private val inventory: SaleInventory,
    private val now: () -> Instant,
    private val retryInterval: Duration = 30.seconds,
) {

    /** `TAKEN` records still owed their stacks; keys are sale ids so a retry never double-restores. */
    private val pending = ConcurrentHashMap<UUID, SaleIntentRecord>()

    /** Classifies every open intent, then owns the retry loop until the session stops. */
    suspend fun recover() {
        classifyOpenIntents()
        attemptRestores()
        kernel.launch {
            while (true) {
                delay(retryInterval)
                attemptRestores()
            }
        }
    }

    /**
     * Restores a mid-session `TAKEN` record whose credit failed; parks it when the owner is already
     * gone so the retry loop can finish the job.
     */
    suspend fun restoreOrPark(record: SaleIntentRecord) {
        if (!settle(record)) {
            pending[record.saleId] = record
        }
    }

    private suspend fun classifyOpenIntents() {
        val open = try {
            repository.loadOpen()
        } catch (_: EconomyDataException) {
            return
        }
        if (open.isEmpty()) return

        val ledgerIds = open.associate { record ->
            record.saleId.toString() to try {
                ledger.transactionId(
                    SELL_ORIGIN,
                    record.saleId.toString()
                )
            } catch (_: EconomyDataException) {
                null
            }
        }

        SaleRecoveryPlan.decide(open) { ledgerIds[it] }.forEach { action ->
            when (action) {
                is SaleRecoveryAction.MarkRolledBack ->
                    runCatching {
                        repository.markRolledBack(
                            action.record.saleId,
                            now()
                        )
                    }

                is SaleRecoveryAction.MarkCommitted ->
                    runCatching {
                        repository.markCommitted(
                            action.record.saleId,
                            action.transactionId,
                            now()
                        )
                    }

                is SaleRecoveryAction.Restore ->
                    pending[action.record.saleId] = action.record
            }
        }
    }

    private suspend fun attemptRestores() {
        pending.values.toList().forEach { record -> settle(record) }
    }

    /** Hands the stacks back and stamps `ROLLED_BACK`; false leaves the record parked. */
    private suspend fun settle(record: SaleIntentRecord): Boolean {
        val restored = runCatching {
            kernel.onServerThread {
                inventory.restore(
                    record.playerId,
                    record.payload
                )
            }
        }.getOrDefault(false)
        if (!restored) return false
        runCatching {
            repository.markRolledBack(
                record.saleId,
                now()
            )
        }
        pending.remove(record.saleId)
        return true
    }

}
