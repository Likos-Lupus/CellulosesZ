package top.likoslupus.cellulosesz.application.api

import org.slf4j.LoggerFactory
import top.likoslupus.cellulosesz.api.ApiQuery
import top.likoslupus.cellulosesz.api.Subscription
import top.likoslupus.cellulosesz.api.actor.AddonAccess
import top.likoslupus.cellulosesz.api.common.ApiCallResult
import top.likoslupus.cellulosesz.api.common.ApiCallback
import top.likoslupus.cellulosesz.api.common.ApiEventListener
import top.likoslupus.cellulosesz.api.common.ApiRequestHandle
import top.likoslupus.cellulosesz.api.economy.*
import top.likoslupus.cellulosesz.api.economy.EconomyTransactionKind
import top.likoslupus.cellulosesz.application.api.extension.ApplicationExtensionHub
import top.likoslupus.cellulosesz.economy.config.EconomySettings
import top.likoslupus.cellulosesz.economy.integration.*
import java.util.*
import java.util.concurrent.CopyOnWriteArrayList
import top.likoslupus.cellulosesz.economy.integration.EconomyTransactionKind as InternalEconomyTransactionKind

/**
 * Adapts the Minecraft-free economy integration port onto the public [EconomyApi]. Every privileged
 * operation is authorized server-side: the caller must present the [AddonAccess] instance the
 * extension hub registered and the live configuration must grant the required capability. A
 * self-implemented access handle is rejected.
 */
internal class EconomyApiAdapter(
    private val lease: SessionLease,
    private val integration: EconomyIntegrationPort,
    private val dispatcher: ApiRequestDispatcher,
    private val hub: ApplicationExtensionHub,
    private val settings: () -> EconomySettings,
) : EconomyApi {

    override val events: EconomyEventsAdapter = EconomyEventsAdapter(lease)

    override fun currency(): ApiQuery<EconomyCurrency> =
        when (lease.phase()) {
            SessionPhase.READY -> {
                val economy = settings()
                ApiQuery.Available(
                    EconomyCurrency(
                        id = "cellulosesz:coin",
                        scale = 2,
                        displayName = economy.currencyName,
                        symbol = economy.currencySymbol,
                    )
                )
            }

            SessionPhase.CREATED -> ApiQuery.NotReady
            SessionPhase.CLOSED -> ApiQuery.Stopping
        }

    override fun balance(
        access: AddonAccess,
        playerId: UUID,
        callback: ApiCallback<EconomyBalanceOutcome>,
    ): ApiRequestHandle =
        submit(
            access,
            CAP_BALANCE_READ,
            EconomyBalanceOutcome.NotAuthorized,
            callback
        ) {
            when (val result = integration.balance(playerId)) {
                is EconomyBalanceResult.Available -> EconomyBalanceOutcome.Available(
                    EconomyAccountView(
                        result.snapshot.playerId,
                        EconomyAmount(result.snapshot.amount),
                        result.snapshot.accountExists
                    )
                )

                EconomyBalanceResult.Disabled -> EconomyBalanceOutcome.Disabled
                EconomyBalanceResult.StorageUnavailable -> EconomyBalanceOutcome.StorageUnavailable
            }
        }

    override fun balanceTop(
        access: AddonAccess,
        callback: ApiCallback<EconomyRankingOutcome>,
    ): ApiRequestHandle =
        submit(
            access,
            CAP_RANKING_READ,
            EconomyRankingOutcome.NotAuthorized,
            callback
        ) {
            when (val result = integration.top(1)) {
                is EconomyRankingResult.Available -> EconomyRankingOutcome.Available(
                    entries = result.rows.mapIndexed { index, row ->
                        EconomyRankingEntryView(
                            (result.page - 1) * result.pageSize + index + 1,
                            row.playerId,
                            EconomyAmount(row.amount)
                        )
                    },
                    page = result.page,
                    pageSize = result.pageSize,
                    total = result.total,
                )

                EconomyRankingResult.Disabled -> EconomyRankingOutcome.Disabled
                EconomyRankingResult.StorageUnavailable -> EconomyRankingOutcome.StorageUnavailable
            }
        }

    override fun history(
        access: AddonAccess,
        query: EconomyHistoryQuery,
        callback: ApiCallback<EconomyHistoryOutcome>,
    ): ApiRequestHandle =
        submit(
            access,
            CAP_HISTORY_READ,
            EconomyHistoryOutcome.NotAuthorized,
            callback
        ) {
            when (val result = integration.history(query.playerId, query.page)) {
                is EconomyHistoryResult.Available -> EconomyHistoryOutcome.Available(
                    entries = result.rows.map {
                        EconomyHistoryEntryView(
                            transactionId = it.transactionId,
                            kind = it.kind.toApiKind(),
                            delta = EconomyAmount(it.delta),
                            balanceAfter = EconomyAmount(it.balanceAfter),
                            occurredAt = it.at,
                        )
                    },
                    page = result.page,
                    pageSize = result.pageSize,
                    total = result.total,
                )

                EconomyHistoryResult.Disabled -> EconomyHistoryOutcome.Disabled
                EconomyHistoryResult.StorageUnavailable -> EconomyHistoryOutcome.StorageUnavailable
            }
        }

    override fun transferAsPlayer(
        request: PlayerEconomyTransferRequest,
        callback: ApiCallback<EconomyTransactionOutcome>,
    ): ApiRequestHandle =
        mutate(
            request.access,
            CAP_PAY_AS_PLAYER,
            EconomyTransactionOutcome.NotAuthorized,
            callback
        ) {
            integration.transfer(
                EconomyTransferRequest(
                    actor = actorFor(request.access, EconomyActorKind.ADDON),
                    payerId = request.payerId,
                    receiverId = request.receiverId,
                    amount = request.amount.decimal,
                    originId = originOf(request.access),
                    requestKey = request.requestKey,
                    reasonCode = "addon.pay",
                    referenceId = request.referenceId,
                )
            ).toOutcome()
        }

    override fun grantByAddon(
        request: AddonEconomyGrantRequest,
        callback: ApiCallback<EconomyTransactionOutcome>,
    ): ApiRequestHandle =
        mutate(
            request.access,
            CAP_GRANT,
            EconomyTransactionOutcome.NotAuthorized,
            callback
        ) {
            integration.grant(
                EconomyGrantRequest(
                    actor = actorFor(request.access, EconomyActorKind.ADDON),
                    playerId = request.playerId,
                    amount = request.amount.decimal,
                    originId = originOf(request.access),
                    requestKey = request.requestKey,
                    reasonCode = request.reasonCode,
                    referenceId = request.referenceId,
                )
            ).toOutcome()
        }

    override fun takeByAddon(
        request: AddonEconomyTakeRequest,
        callback: ApiCallback<EconomyTransactionOutcome>,
    ): ApiRequestHandle =
        mutate(
            request.access,
            CAP_TAKE,
            EconomyTransactionOutcome.NotAuthorized,
            callback
        ) {
            integration.take(
                EconomyTakeRequest(
                    actor = actorFor(request.access, EconomyActorKind.ADDON),
                    playerId = request.playerId,
                    amount = request.amount.decimal,
                    originId = originOf(request.access),
                    requestKey = request.requestKey,
                    reasonCode = request.reasonCode,
                    referenceId = request.referenceId,
                )
            ).toOutcome()
        }

    override fun setByAddon(
        request: AddonEconomySetRequest,
        callback: ApiCallback<EconomyTransactionOutcome>,
    ): ApiRequestHandle =
        mutate(
            request.access,
            CAP_SET,
            EconomyTransactionOutcome.NotAuthorized,
            callback
        ) {
            integration.set(
                EconomySetRequest(
                    actor = actorFor(request.access, EconomyActorKind.ADDON),
                    playerId = request.playerId,
                    target = request.targetBalance.decimal,
                    originId = originOf(request.access),
                    requestKey = request.requestKey,
                    reasonCode = request.reasonCode,
                    referenceId = request.referenceId,
                )
            ).toOutcome()
        }

    /**
     * Runs [task] only after authorizing [capability], and delivers its single completion on the
     * server thread. An unauthorized caller gets a terminal [ApiCallResult.Completed] with a
     * domain [denied] value; it never reaches the ledger.
     */
    private fun <T> submit(
        access: AddonAccess,
        capability: String,
        denied: T,
        callback: ApiCallback<T>,
        task: suspend () -> T,
    ): ApiRequestHandle =
        when {
            !hub.isAuthorizedAccess(access, capability) ->
                dispatcher.submit(
                    task = { ApiCallResult.Completed(denied) },
                    deliver = callback::onComplete,
                )

            else ->
                dispatcher.submit(
                    task = { ApiCallResult.Completed(task()) },
                    deliver = callback::onComplete,
                )
        }

    /**
     * Runs a money-moving [task] behind the same authorization, but through the commit-fenced
     * dispatcher: a cancellation succeeds only before the ledger is touched, and a request that
     * reached the commit region conservatively refuses cancellation so the caller recovers by
     * request key instead of risking a duplicate move.
     */
    private fun <T> mutate(
        access: AddonAccess,
        capability: String,
        denied: T,
        callback: ApiCallback<T>,
        task: suspend () -> T,
    ): ApiRequestHandle =
        when {
            !hub.isAuthorizedAccess(access, capability) ->
                dispatcher.submit(
                    task = { ApiCallResult.Completed(denied) },
                    deliver = callback::onComplete,
                )

            else ->
                dispatcher.submitMoney(
                    task = { fence ->
                        when {
                            !fence.tryBeginCommit() -> ApiCallResult.NotReady
                            else -> ApiCallResult.Completed(task())
                        }
                    },
                    deliver = callback::onComplete,
                )
        }

    private fun actorFor(access: AddonAccess, kind: EconomyActorKind): EconomyActorDescriptor =
        EconomyActorDescriptor(kind, access.addonId)

    private fun originOf(access: AddonAccess): String =
        "addon:${access.addonId}"

    private companion object {

        private const val CAP_BALANCE_READ = "economy.balance.read"

        private const val CAP_RANKING_READ = "economy.ranking.read"

        private const val CAP_HISTORY_READ = "economy.history.read"

        private const val CAP_PAY_AS_PLAYER = "economy.pay.as-player"

        private const val CAP_GRANT = "economy.grant"

        private const val CAP_TAKE = "economy.take"

        private const val CAP_SET = "economy.set"

    }

}

private fun EconomyTransactionResult.toOutcome(): EconomyTransactionOutcome =
    when (this) {
        is EconomyTransactionResult.Committed ->
            EconomyTransactionOutcome.Committed(receipt.toView())

        is EconomyTransactionResult.AlreadyCommitted ->
            EconomyTransactionOutcome.AlreadyCommitted(
                receipt.toView()
            )

        is EconomyTransactionResult.NoChange ->
            EconomyTransactionOutcome.NoChange(
                transactionId,
                EconomyAmount(balance)
            )

        EconomyTransactionResult.InsufficientFunds ->
            EconomyTransactionOutcome.InsufficientFunds

        EconomyTransactionResult.ExceedsBalanceLimit ->
            EconomyTransactionOutcome.ExceedsBalanceLimit

        EconomyTransactionResult.InvalidAmount ->
            EconomyTransactionOutcome.InvalidAmount

        EconomyTransactionResult.TransferLimitExceeded ->
            EconomyTransactionOutcome.InvalidAmount

        EconomyTransactionResult.SameAccount ->
            EconomyTransactionOutcome.SameAccount

        EconomyTransactionResult.IdempotencyConflict ->
            EconomyTransactionOutcome.IdempotencyConflict

        EconomyTransactionResult.Disabled ->
            EconomyTransactionOutcome.Disabled

        EconomyTransactionResult.StorageUnavailable ->
            EconomyTransactionOutcome.StorageUnavailable
    }

private fun EconomyTransactionReceipt.toView(): EconomyTransactionReceiptView =
    EconomyTransactionReceiptView(
        transactionId = transactionId,
        kind = kind.toApiKind(),
        occurredAt = occurredAt,
        entries = entries.map {
            EconomyTransactionEntryView(
                it.playerId,
                EconomyAmount(it.delta),
                EconomyAmount(it.balanceAfter)
            )
        },
    )

private fun InternalEconomyTransactionKind.toApiKind(): EconomyTransactionKind =
    EconomyTransactionKind.valueOf(name)

/**
 * Session-scoped fan-out of committed economy facts. Only dispatches while the session is ready; a
 * bad listener is isolated, and a subscriber added after the session closed gets an inert handle.
 */
internal class EconomyEventsAdapter(
    private val lease: SessionLease,
) : EconomyEvents {

    private val transactionListeners = CopyOnWriteArrayList<ApiEventListener<EconomyTransactionCommitted>>()
    private val balanceListeners = CopyOnWriteArrayList<ApiEventListener<EconomyBalanceChanged>>()
    private val accountListeners = CopyOnWriteArrayList<ApiEventListener<EconomyAccountCreated>>()

    override fun onTransactionCommitted(
        listener: ApiEventListener<EconomyTransactionCommitted>,
    ): Subscription = register(transactionListeners, listener)

    override fun onBalanceChanged(
        listener: ApiEventListener<EconomyBalanceChanged>,
    ): Subscription = register(balanceListeners, listener)

    override fun onAccountCreated(
        listener: ApiEventListener<EconomyAccountCreated>,
    ): Subscription = register(accountListeners, listener)

    fun dispatchCommitted(event: EconomyTransactionCommitted) {
        if (!lease.isReady) {
            return
        }
        transactionListeners.forEach { listener ->
            runCatching { listener.onEvent(event) }
                    .onFailure { LOGGER.warn("api_economy_listener_failed kind=transaction", it) }
        }
    }

    fun dispatchBalanceChanged(event: EconomyBalanceChanged) {
        if (!lease.isReady) {
            return
        }
        balanceListeners.forEach { listener ->
            runCatching { listener.onEvent(event) }
                    .onFailure { LOGGER.warn("api_economy_listener_failed kind=balance", it) }
        }
    }

    fun dispatchAccountCreated(event: EconomyAccountCreated) {
        if (!lease.isReady) {
            return
        }
        accountListeners.forEach { listener ->
            runCatching { listener.onEvent(event) }
                    .onFailure { LOGGER.warn("api_economy_listener_failed kind=account", it) }
        }
    }

    fun clear() {
        transactionListeners.clear()
        balanceListeners.clear()
        accountListeners.clear()
    }

    private fun <T> register(
        list: CopyOnWriteArrayList<ApiEventListener<T>>,
        listener: ApiEventListener<T>,
    ): Subscription {
        if (lease.phase() == SessionPhase.CLOSED) {
            return Subscription { }
        }
        list += listener
        return Subscription { list.remove(listener) }
    }

    private companion object {

        private val LOGGER = LoggerFactory.getLogger(EconomyEventsAdapter::class.java)

    }

}
