package top.likoslupus.cellulosesz.economy.service

import top.likoslupus.cellulosesz.economy.ledger.ActorKind
import top.likoslupus.cellulosesz.economy.ledger.EconomyReceipt
import top.likoslupus.cellulosesz.economy.ledger.HistoryEntry
import top.likoslupus.cellulosesz.economy.ledger.TopEntry
import top.likoslupus.cellulosesz.economy.money.Money
import java.util.*

/** The attributed initiator of an economy operation. */
internal data class EconomyActor(
    val kind: ActorKind,
    val id: String,
)

/** A player-to-player transfer request. [requestKey] is stable per business intent. */
internal data class TransferIntent(
    val actor: EconomyActor,
    val payerId: UUID,
    val receiverId: UUID,
    val amount: Money,
    val originId: String,
    val requestKey: String,
    val reasonCode: String,
    val referenceId: String? = null,
)

/** A system issuance ([ActorKind.ADDON]/[ActorKind.ADMIN]) minting currency to a player. */
internal data class GrantIntent(
    val actor: EconomyActor,
    val playerId: UUID,
    val amount: Money,
    val originId: String,
    val requestKey: String,
    val reasonCode: String,
    val referenceId: String? = null,
)

/** A system reclaim withdrawing currency from a player into the sink. */
internal data class TakeIntent(
    val actor: EconomyActor,
    val playerId: UUID,
    val amount: Money,
    val originId: String,
    val requestKey: String,
    val reasonCode: String,
    val referenceId: String? = null,
)

/** An administrator adjustment of a player's balance to an exact target. */
internal data class SetIntent(
    val actor: EconomyActor,
    val playerId: UUID,
    val target: Money,
    val originId: String,
    val requestKey: String,
    val reasonCode: String,
    val referenceId: String? = null,
)

/** The typed result of a mutating economy operation. Every branch is a normal business outcome. */
internal sealed interface EconomyMutationResult {

    data class Committed(val receipt: EconomyReceipt) : EconomyMutationResult

    data class AlreadyCommitted(val receipt: EconomyReceipt) : EconomyMutationResult

    data class NoChange(val transactionId: UUID, val balance: Money) : EconomyMutationResult

    data object Disabled : EconomyMutationResult

    data object SameAccount : EconomyMutationResult

    data object InvalidAmount : EconomyMutationResult

    data object InsufficientFunds : EconomyMutationResult

    data object BalanceLimitExceeded : EconomyMutationResult

    data object TransferLimitExceeded : EconomyMutationResult

    data object IdempotencyConflict : EconomyMutationResult

    data object TemporarilyUnavailable : EconomyMutationResult

}

internal sealed interface BalanceResult {

    data class Balance(
        val playerId: UUID,
        val amount: Money,
        val accountExists: Boolean,
    ) : BalanceResult

    data object Disabled : BalanceResult

    data object StorageUnavailable : BalanceResult

}

internal sealed interface HistoryResult {

    data class Page(
        val entries: List<HistoryEntry>,
        val total: Int,
        val page: Int,
        val pageSize: Int,
    ) : HistoryResult

    data object Disabled : HistoryResult

    data object StorageUnavailable : HistoryResult

}

internal sealed interface RankingResult {

    data class Page(
        val entries: List<TopEntry>,
        val total: Int,
        val page: Int,
        val pageSize: Int,
    ) : RankingResult

    data object Disabled : RankingResult

    data object StorageUnavailable : RankingResult

}
