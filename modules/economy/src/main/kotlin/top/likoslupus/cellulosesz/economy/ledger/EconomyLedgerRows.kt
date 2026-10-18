package top.likoslupus.cellulosesz.economy.ledger

import top.likoslupus.cellulosesz.economy.account.AccountKind
import java.util.UUID

/**
 * Thrown inside a transaction to force a rollback while returning a typed business result.
 *
 * It deliberately carries no stack trace: it is control flow, not a failure.
 */
internal class LedgerAbort(
    val result: LedgerCommitResult
) : RuntimeException(null, null, false, false)

/** The persisted identity/state of one account row, read before a mutation. */
internal class AccountRow(
    val kind: AccountKind,
    val balanceMinor: Long,
    val revision: Long
)

/** The idempotency identity of an already-committed transaction row. */
internal class TransactionRow(
    val transactionId: UUID,
    val requestHash: String
)
