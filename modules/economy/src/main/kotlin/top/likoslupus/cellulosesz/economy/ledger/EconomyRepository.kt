package top.likoslupus.cellulosesz.economy.ledger

import top.likoslupus.cellulosesz.economy.account.EconomyAccountId
import top.likoslupus.cellulosesz.economy.money.Money
import java.time.Instant
import java.util.*

/**
 * The single owner of every economy write. Each mutation commits in one database transaction: the
 * guarded balance updates, the transaction header and the balanced entries either all persist or
 * none does. Callers never see a JDBC [java.sql.Connection].
 */
internal interface EconomyRepository {

    /** Idempotently creates a player account; applies the opening balance exactly once. */
    suspend fun ensurePlayerAccount(
        playerId: UUID,
        openingAmount: Money,
        maximum: Money,
        at: Instant,
    ): AccountOpenResult

    /** Reads one account snapshot, or null when the account does not exist. */
    suspend fun account(accountId: EconomyAccountId): StoredAccountView?

    /**
     * Commits one balanced transaction. [lines] must be non-empty and sum to zero. Returns the
     * existing receipt when the same `(origin, requestKey)` was already committed with the same
     * fingerprint. [maximum] is the configured per-account credit ceiling for player lines.
     */
    suspend fun commit(
        header: LedgerHeader,
        lines: List<LedgerLine>,
        maximum: Money,
    ): LedgerCommitResult

    /**
     * Sets a player balance to [target] using optimistic concurrency. A zero delta is recorded as a
     * durable [TransactionKind.SET_NO_CHANGE] decision rather than a monetary movement.
     */
    suspend fun setBalance(
        header: LedgerHeader,
        playerId: UUID,
        target: Money,
    ): LedgerCommitResult

    /** A page of one account's history, newest first. */
    suspend fun history(
        playerId: UUID,
        offset: Int,
        limit: Int
    ): HistoryPage

    /** A page of player accounts ranked by balance, highest first. */
    suspend fun top(
        offset: Int,
        limit: Int,
        includeZero: Boolean
    ): TopPage

    /**
     * The committed transaction id for `(originId, requestKey)`, or null when no such transaction
     * exists. Used by sale recovery to tell a landed credit from a missing one without writing.
     */
    suspend fun transactionId(originId: String, requestKey: String): UUID?

}
