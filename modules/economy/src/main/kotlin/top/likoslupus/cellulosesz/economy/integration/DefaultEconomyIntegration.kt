package top.likoslupus.cellulosesz.economy.integration

import top.likoslupus.cellulosesz.economy.account.EconomyAccountId
import top.likoslupus.cellulosesz.economy.ledger.ActorKind
import top.likoslupus.cellulosesz.economy.ledger.EconomyReceipt
import top.likoslupus.cellulosesz.economy.ledger.TransactionKind
import top.likoslupus.cellulosesz.economy.money.Money
import top.likoslupus.cellulosesz.economy.service.*
import top.likoslupus.cellulosesz.economy.worth.WorthService
import java.math.BigDecimal
import java.util.*

/**
 * Adapts the Minecraft-free [EconomyIntegrationPort] onto the single [EconomyService] and forwards
 * committed facts to the [observer]. Only a genuinely new commit emits an event; an idempotent
 * replay does not.
 */
internal class DefaultEconomyIntegration(
    private val service: EconomyService,
    private val observer: EconomyDomainObserver,
    private val worth: WorthService,
) : EconomyIntegrationPort {

    override suspend fun balance(playerId: UUID): EconomyBalanceResult =
        when (val result = service.balance(playerId)) {
            is BalanceResult.Balance -> EconomyBalanceResult.Available(
                EconomyAccountSnapshot(
                    result.playerId,
                    result.amount.decimal,
                    result.accountExists
                )
            )

            BalanceResult.Disabled -> EconomyBalanceResult.Disabled
            BalanceResult.StorageUnavailable -> EconomyBalanceResult.StorageUnavailable
        }

    override suspend fun ensureAccount(playerId: UUID): Boolean =
        service.ensureAccount(playerId)

    override suspend fun transfer(intent: EconomyTransferRequest): EconomyTransactionResult =
        service.transfer(
            TransferIntent(
                actor = intent.actor.toInternal(),
                payerId = intent.payerId,
                receiverId = intent.receiverId,
                amount = Money.of(intent.amount),
                originId = intent.originId,
                requestKey = intent.requestKey,
                reasonCode = intent.reasonCode,
                referenceId = intent.referenceId,
            )
        ).toPort()

    override suspend fun grant(intent: EconomyGrantRequest): EconomyTransactionResult =
        service.grant(
            GrantIntent(
                actor = intent.actor.toInternal(),
                playerId = intent.playerId,
                amount = Money.of(intent.amount),
                originId = intent.originId,
                requestKey = intent.requestKey,
                reasonCode = intent.reasonCode,
                referenceId = intent.referenceId,
            )
        ).toPort()

    override suspend fun take(intent: EconomyTakeRequest): EconomyTransactionResult =
        service.take(
            TakeIntent(
                actor = intent.actor.toInternal(),
                playerId = intent.playerId,
                amount = Money.of(intent.amount),
                originId = intent.originId,
                requestKey = intent.requestKey,
                reasonCode = intent.reasonCode,
                referenceId = intent.referenceId,
            )
        ).toPort()

    override suspend fun set(intent: EconomySetRequest): EconomyTransactionResult =
        service.set(
            SetIntent(
                actor = intent.actor.toInternal(),
                playerId = intent.playerId,
                target = Money.of(intent.target),
                originId = intent.originId,
                requestKey = intent.requestKey,
                reasonCode = intent.reasonCode,
                referenceId = intent.referenceId,
            )
        ).toPort()

    override suspend fun history(playerId: UUID, page: Int): EconomyHistoryResult =
        when (
            val result = service.history(
                playerId,
                page,
                pageSize = HISTORY_PAGE_SIZE
            )
        ) {
            is HistoryResult.Page -> EconomyHistoryResult.Available(
                rows = result.entries.map { entry ->
                    EconomyHistoryRow(
                        transactionId = entry.transactionId,
                        kind = entry.kind.toPort(),
                        delta = entry.delta.decimal,
                        balanceAfter = entry.balanceAfter.decimal,
                        at = entry.at,
                        reasonCode = entry.reasonCode,
                        referenceId = entry.referenceId,
                    )
                },
                total = result.total,
                page = result.page,
                pageSize = result.pageSize,
            )

            HistoryResult.Disabled -> EconomyHistoryResult.Disabled
            HistoryResult.StorageUnavailable -> EconomyHistoryResult.StorageUnavailable
        }

    override suspend fun top(page: Int): EconomyRankingResult =
        when (
            val result = service.top(
                page,
                pageSize = RANKING_PAGE_SIZE
            )
        ) {
            is RankingResult.Page -> EconomyRankingResult.Available(
                rows = result.entries.map { entry ->
                    EconomyRankingRow(
                        entry.playerId,
                        entry.balance.decimal
                    )
                },
                total = result.total,
                page = result.page,
                pageSize = result.pageSize,
            )

            RankingResult.Disabled -> EconomyRankingResult.Disabled
            RankingResult.StorageUnavailable -> EconomyRankingResult.StorageUnavailable
        }

    override suspend fun worthUnitPrice(itemId: String): BigDecimal? =
        worth.unitPrice(itemId)?.decimal

    override suspend fun worthCatalog(): List<EconomyWorthView> =
        worth.entries().map {
            EconomyWorthView(
                it.itemId,
                it.price.decimal
            )
        }

    override suspend fun setWorth(
        itemId: String,
        unitPrice: BigDecimal,
        actor: EconomyActorDescriptor,
    ): Boolean =
        worth.set(
            itemId,
            Money.of(unitPrice),
            actor.id
        )

    private fun EconomyMutationResult.toPort(): EconomyTransactionResult =
        when (this) {
            is EconomyMutationResult.Committed -> {
                observer.onTransactionCommitted(receipt.toEvent())
                EconomyTransactionResult.Committed(receipt.toPort())
            }

            is EconomyMutationResult.AlreadyCommitted ->
                EconomyTransactionResult.AlreadyCommitted(
                    receipt.toPort()
                )

            is EconomyMutationResult.NoChange ->
                EconomyTransactionResult.NoChange(
                    transactionId,
                    balance.decimal
                )

            EconomyMutationResult.Disabled -> EconomyTransactionResult.Disabled
            EconomyMutationResult.SameAccount -> EconomyTransactionResult.SameAccount
            EconomyMutationResult.InvalidAmount -> EconomyTransactionResult.InvalidAmount
            EconomyMutationResult.InsufficientFunds -> EconomyTransactionResult.InsufficientFunds
            EconomyMutationResult.BalanceLimitExceeded -> EconomyTransactionResult.ExceedsBalanceLimit
            EconomyMutationResult.TransferLimitExceeded -> EconomyTransactionResult.TransferLimitExceeded
            EconomyMutationResult.IdempotencyConflict -> EconomyTransactionResult.IdempotencyConflict
            EconomyMutationResult.TemporarilyUnavailable -> EconomyTransactionResult.StorageUnavailable
        }

    private fun EconomyReceipt.toPort(): EconomyTransactionReceipt =
        EconomyTransactionReceipt(
            transactionId = transactionId,
            kind = kind.toPort(),
            occurredAt = at,
            entries = entries.mapNotNull { entry ->
                val player = entry.accountId as? EconomyAccountId.Player
                    ?: return@mapNotNull null
                EconomyEntryView(
                    player.playerUuid,
                    entry.delta.decimal,
                    entry.balanceAfter.decimal
                )
            },
        )

    private fun EconomyReceipt.toEvent(): EconomyCommittedEvent =
        EconomyCommittedEvent(
            transactionId = transactionId,
            kind = kind.toPort(),
            currencyId = currencyId,
            occurredAt = at,
            changes = entries.mapNotNull { entry ->
                val player = entry.accountId as? EconomyAccountId.Player
                    ?: return@mapNotNull null
                EconomyBalanceChange(
                    player.playerUuid,
                    entry.delta.decimal
                )
            },
        )

    private fun TransactionKind.toPort(): EconomyTransactionKind =
        EconomyTransactionKind.valueOf(name)

    private fun EconomyActorDescriptor.toInternal(): EconomyActor =
        EconomyActor(ActorKind.valueOf(kind.name), id)

    private companion object {

        private const val HISTORY_PAGE_SIZE = 10

        private const val RANKING_PAGE_SIZE = 10

    }

}
