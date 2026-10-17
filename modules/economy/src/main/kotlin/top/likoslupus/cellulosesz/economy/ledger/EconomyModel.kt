package top.likoslupus.cellulosesz.economy.ledger

import top.likoslupus.cellulosesz.economy.account.AccountKind
import top.likoslupus.cellulosesz.economy.account.EconomyAccountId
import top.likoslupus.cellulosesz.economy.money.Money
import java.time.Instant
import java.util.*

/** The single native currency. Its id and scale are stable and are never configuration-tunable. */
internal const val CURRENCY_ID: String = "cellulosesz:coin"

/** Fixed decimal scale for every monetary value; kept in step with [Money]. */
internal const val CURRENCY_SCALE: Int = 2

/** The business shape of a committed transaction row. */
internal enum class TransactionKind {

    OPENING,
    TRANSFER,
    GRANT,
    TAKE,
    SET,
    SET_NO_CHANGE,
    COMPENSATION,

}

/** Who initiated a transaction; an attribution label, never a cryptographic token. */
internal enum class ActorKind {

    PLAYER,
    ADMIN,
    ADDON,
    INTERNAL,

}

/** A committed, immutable ledger entry describing one account's signed change. */
internal data class RecordedEntry(
    val accountId: EconomyAccountId,
    val delta: Money,
    val balanceAfter: Money,
)

/** The persisted confirmation of one committed transaction. */
internal data class EconomyReceipt(
    val transactionId: UUID,
    val kind: TransactionKind,
    val currencyId: String,
    val at: Instant,
    val entries: List<RecordedEntry>,
)

/** A read-only snapshot of an account row. */
internal data class StoredAccountView(
    val accountId: EconomyAccountId,
    val kind: AccountKind,
    val balance: Money,
    val revision: Long,
)

/** The idempotency identity and attribution of one requested transaction. */
internal data class LedgerHeader(
    val kind: TransactionKind,
    val originId: String,
    val requestKey: String,
    val requestHash: String,
    val actorKind: ActorKind,
    val actorId: String,
    val reasonCode: String,
    val referenceId: String?,
    val createdAt: Instant,
)

/** The role of one ledger line: it fixes the sign, the guard and the permitted account kind. */
internal enum class LedgerLineRole {

    DEBIT_PLAYER,
    CREDIT_PLAYER,
    SYSTEM_DEBIT,
    SYSTEM_CREDIT,

}

/** One balanced side of a transaction; [amount] is always positive, the sign comes from [role]. */
internal data class LedgerLine(
    val accountId: EconomyAccountId,
    val amount: Money,
    val role: LedgerLineRole,
) {

    /** The signed balance change this line applies to its account. */
    val signedDelta: Money
        get() = when (role) {
            LedgerLineRole.DEBIT_PLAYER,
            LedgerLineRole.SYSTEM_DEBIT -> amount.negate()

            LedgerLineRole.CREDIT_PLAYER,
            LedgerLineRole.SYSTEM_CREDIT -> amount
        }

}

/** The outcome of a repository commit or a balance set. */
internal sealed interface LedgerCommitResult {

    data class Committed(val receipt: EconomyReceipt) : LedgerCommitResult

    data class AlreadyCommitted(val receipt: EconomyReceipt) : LedgerCommitResult

    data class NoChange(val transactionId: UUID, val balance: Money) : LedgerCommitResult

    data object InsufficientFunds : LedgerCommitResult

    data object BalanceLimitExceeded : LedgerCommitResult

    data object IdempotencyConflict : LedgerCommitResult

    data object ConcurrentModification : LedgerCommitResult

}

/** Whether account creation inserted a fresh row or found an existing one. */
internal sealed interface AccountOpenResult {

    data object Opened : AccountOpenResult

    data object Existing : AccountOpenResult

}

/** One account-scoped history row. */
internal data class HistoryEntry(
    val transactionId: UUID,
    val kind: TransactionKind,
    val delta: Money,
    val balanceAfter: Money,
    val at: Instant,
    val originId: String,
    val reasonCode: String,
    val referenceId: String?,
)

internal data class HistoryPage(
    val entries: List<HistoryEntry>,
    val total: Int,
)

internal data class TopEntry(
    val playerId: UUID,
    val balance: Money,
)

internal data class TopPage(
    val entries: List<TopEntry>,
    val total: Int,
)
