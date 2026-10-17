package top.likoslupus.cellulosesz.economy.service

import top.likoslupus.cellulosesz.economy.account.EconomyAccountId
import top.likoslupus.cellulosesz.economy.config.EconomySettings
import top.likoslupus.cellulosesz.economy.ledger.*
import top.likoslupus.cellulosesz.economy.money.Money
import java.security.MessageDigest
import java.time.Instant
import java.util.*

/**
 * The single business entry for every economic mutation and query. Commands, administration tooling
 * and the public API all funnel through these methods; none writes a balance directly. Amount
 * validation, feature enablement, account opening and idempotency fingerprints live here, while the
 * repository owns the atomic commit.
 */
internal class EconomyService(
    private val repository: EconomyRepository,
    private val settings: () -> EconomySettings,
    private val now: () -> Instant,
) {

    private val maximum: Money
        get() = Money.of(settings().maximumBalance)

    private val starting: Money
        get() = Money.of(settings().startingBalance)

    private val minimumPayment: Money
        get() = Money.of(settings().minimumPayment)

    private val maximumPayment: Money
        get() = Money.of(settings().maximumPayment)

    /** Ensures the player has an account, applying the opening balance at most once. */
    suspend fun ensureAccount(playerId: UUID): Boolean =
        try {
            repository.ensurePlayerAccount(playerId, starting, maximum, now())
            true
        } catch (_: EconomyDataException) {
            false
        }

    suspend fun balance(playerId: UUID): BalanceResult =
        when {
            !settings().enabled -> BalanceResult.Disabled
            else -> try {
                val view = repository.account(EconomyAccountId.Player(playerId))
                BalanceResult.Balance(
                    playerId = playerId,
                    amount = view?.balance ?: Money.ZERO,
                    accountExists = view != null,
                )
            } catch (_: EconomyDataException) {
                BalanceResult.StorageUnavailable
            }
        }

    suspend fun transfer(intent: TransferIntent): EconomyMutationResult =
        when {
            !settings().enabled ->
                EconomyMutationResult.Disabled

            intent.payerId == intent.receiverId ->
                EconomyMutationResult.SameAccount

            !intent.amount.isPositive ->
                EconomyMutationResult.InvalidAmount

            intent.amount.compareTo(minimumPayment) < 0
                    || intent.amount.compareTo(maximumPayment) > 0 ->
                EconomyMutationResult.TransferLimitExceeded

            !openAccounts(intent.payerId, intent.receiverId) ->
                EconomyMutationResult.TemporarilyUnavailable

            else -> {
                val lines = listOf(
                    LedgerLine(
                        EconomyAccountId.Player(intent.payerId),
                        intent.amount,
                        LedgerLineRole.DEBIT_PLAYER
                    ),
                    LedgerLine(
                        EconomyAccountId.Player(intent.receiverId),
                        intent.amount,
                        LedgerLineRole.CREDIT_PLAYER
                    ),
                )
                val header = header(
                    kind = TransactionKind.TRANSFER,
                    originId = intent.originId,
                    requestKey = intent.requestKey,
                    actor = intent.actor,
                    reasonCode = intent.reasonCode,
                    referenceId = intent.referenceId,
                    fingerprintParts = listOf(
                        intent.payerId.toString(),
                        intent.receiverId.toString(),
                        intent.amount.toString(),
                    ),
                )
                commit(header, lines)
            }
        }

    suspend fun grant(intent: GrantIntent): EconomyMutationResult =
        when {
            !settings().enabled ->
                EconomyMutationResult.Disabled

            !intent.amount.isPositive || intent.amount.compareTo(maximum) > 0 ->
                EconomyMutationResult.InvalidAmount

            !openAccounts(intent.playerId) ->
                EconomyMutationResult.TemporarilyUnavailable

            else -> {
                val lines = listOf(
                    LedgerLine(
                        EconomyAccountId.Mint,
                        intent.amount,
                        LedgerLineRole.SYSTEM_DEBIT
                    ),
                    LedgerLine(
                        EconomyAccountId.Player(intent.playerId),
                        intent.amount,
                        LedgerLineRole.CREDIT_PLAYER
                    ),
                )
                val header = header(
                    kind = TransactionKind.GRANT,
                    originId = intent.originId,
                    requestKey = intent.requestKey,
                    actor = intent.actor,
                    reasonCode = intent.reasonCode,
                    referenceId = intent.referenceId,
                    fingerprintParts = listOf(
                        intent.playerId.toString(),
                        intent.amount.toString(),
                    ),
                )
                commit(header, lines)
            }
        }

    suspend fun take(intent: TakeIntent): EconomyMutationResult =
        when {
            !settings().enabled ->
                EconomyMutationResult.Disabled

            !intent.amount.isPositive || intent.amount.compareTo(maximum) > 0 ->
                EconomyMutationResult.InvalidAmount

            !openAccounts(intent.playerId) ->
                EconomyMutationResult.TemporarilyUnavailable

            else -> {
                val lines = listOf(
                    LedgerLine(
                        EconomyAccountId.Player(intent.playerId),
                        intent.amount,
                        LedgerLineRole.DEBIT_PLAYER
                    ),
                    LedgerLine(
                        EconomyAccountId.Sink,
                        intent.amount,
                        LedgerLineRole.SYSTEM_CREDIT
                    ),
                )
                val header = header(
                    kind = TransactionKind.TAKE,
                    originId = intent.originId,
                    requestKey = intent.requestKey,
                    actor = intent.actor,
                    reasonCode = intent.reasonCode,
                    referenceId = intent.referenceId,
                    fingerprintParts = listOf(
                        intent.playerId.toString(),
                        intent.amount.toString(),
                    ),
                )
                return commit(header, lines)
            }
        }


    suspend fun set(intent: SetIntent): EconomyMutationResult =
        when {
            !settings().enabled ->
                EconomyMutationResult.Disabled

            !intent.target.isNonNegative
                    || intent.target.compareTo(maximum) > 0 ->
                EconomyMutationResult.InvalidAmount

            !openAccounts(intent.playerId) ->
                EconomyMutationResult.TemporarilyUnavailable

            else -> {
                val header = header(
                    kind = TransactionKind.SET,
                    originId = intent.originId,
                    requestKey = intent.requestKey,
                    actor = intent.actor,
                    reasonCode = intent.reasonCode,
                    referenceId = intent.referenceId,
                    fingerprintParts = listOf(
                        intent.playerId.toString(),
                        intent.target.toString(),
                    ),
                )
                return try {
                    repository.setBalance(
                        header,
                        intent.playerId,
                        intent.target
                    ).toMutationResult()
                } catch (_: EconomyDataException) {
                    EconomyMutationResult.TemporarilyUnavailable
                }
            }

        }

    suspend fun history(
        playerId: UUID,
        page: Int,
        pageSize: Int
    ): HistoryResult {
        if (!settings().enabled) {
            return HistoryResult.Disabled
        }
        val size = pageSize.coerceIn(1, 50)
        val safePage = page.coerceAtLeast(1)
        return try {
            val result = repository.history(
                playerId,
                (safePage - 1) * size,
                size
            )
            HistoryResult.Page(
                result.entries,
                result.total,
                safePage,
                size
            )
        } catch (_: EconomyDataException) {
            HistoryResult.StorageUnavailable
        }
    }

    suspend fun top(page: Int, pageSize: Int): RankingResult {
        if (!settings().enabled) {
            return RankingResult.Disabled
        }
        val size = pageSize.coerceIn(1, 50)
        val safePage = page.coerceAtLeast(1)
        return try {
            val result = repository.top(
                offset = (safePage - 1) * size,
                limit = size,
                includeZero = settings().showZeroBalancesInTop,
            )
            RankingResult.Page(
                result.entries,
                result.total,
                safePage,
                size
            )
        } catch (_: EconomyDataException) {
            RankingResult.StorageUnavailable
        }
    }

    private suspend fun openAccounts(vararg playerIds: UUID): Boolean =
        try {
            playerIds.forEach {
                repository.ensurePlayerAccount(
                    it,
                    starting,
                    maximum,
                    now()
                )
            }
            true
        } catch (_: EconomyDataException) {
            false
        }

    private suspend fun commit(
        header: LedgerHeader,
        lines: List<LedgerLine>
    ): EconomyMutationResult =
        try {
            repository.commit(header, lines, maximum).toMutationResult()
        } catch (_: EconomyDataException) {
            EconomyMutationResult.TemporarilyUnavailable
        }

    private fun header(
        kind: TransactionKind,
        originId: String,
        requestKey: String,
        actor: EconomyActor,
        reasonCode: String,
        referenceId: String?,
        fingerprintParts: List<String>,
    ): LedgerHeader =
        LedgerHeader(
            kind = kind,
            originId = originId,
            requestKey = requestKey,
            requestHash = fingerprint(kind, fingerprintParts),
            actorKind = actor.kind,
            actorId = actor.id,
            reasonCode = reasonCode,
            referenceId = referenceId,
            createdAt = now(),
        )

    private fun fingerprint(kind: TransactionKind, parts: List<String>): String {
        val canonical = (listOf(kind.name) + parts)
                .joinToString("|") {
                    "${it.length}:$it"
                }
        val digest = MessageDigest.getInstance("SHA-256")
                .digest(canonical.toByteArray(Charsets.UTF_8))
        return digest.joinToString("") {
            "%02x".format(it)
        }
    }

    private fun LedgerCommitResult.toMutationResult(): EconomyMutationResult =
        when (this) {
            is LedgerCommitResult.Committed -> EconomyMutationResult.Committed(receipt)
            is LedgerCommitResult.AlreadyCommitted -> EconomyMutationResult.AlreadyCommitted(receipt)
            is LedgerCommitResult.NoChange -> EconomyMutationResult.NoChange(transactionId, balance)
            LedgerCommitResult.InsufficientFunds -> EconomyMutationResult.InsufficientFunds
            LedgerCommitResult.BalanceLimitExceeded -> EconomyMutationResult.BalanceLimitExceeded
            LedgerCommitResult.IdempotencyConflict -> EconomyMutationResult.IdempotencyConflict
            LedgerCommitResult.ConcurrentModification -> EconomyMutationResult.TemporarilyUnavailable
        }

}
