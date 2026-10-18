package top.likoslupus.cellulosesz.api.economy

import top.likoslupus.cellulosesz.api.ApiQuery
import top.likoslupus.cellulosesz.api.Subscription
import top.likoslupus.cellulosesz.api.actor.AddonAccess
import top.likoslupus.cellulosesz.api.common.ApiCallback
import top.likoslupus.cellulosesz.api.common.ApiEventListener
import top.likoslupus.cellulosesz.api.common.ApiRequestHandle
import java.time.Instant
import java.util.*

/** Stable metadata of the single native currency. */
public class EconomyCurrency(
    public val id: String,
    public val scale: Int,
    public val displayName: String,
    public val symbol: String,
)

/** A read-only balance snapshot. */
public class EconomyAccountView(
    public val playerId: UUID,
    public val balance: EconomyAmount,
    public val accountExists: Boolean,
)

/** The business shape of a committed transaction. */
public enum class EconomyTransactionKind {

    OPENING,
    TRANSFER,
    GRANT,
    TAKE,
    SET,
    SET_NO_CHANGE,
    COMPENSATION,

}

/** A player's signed change within a committed transaction. */
public class EconomyTransactionEntryView(
    public val playerId: UUID,
    public val delta: EconomyAmount,
    public val balanceAfter: EconomyAmount,
)

/** A committed transaction receipt. */
public class EconomyTransactionReceiptView(
    public val transactionId: UUID,
    public val kind: EconomyTransactionKind,
    public val occurredAt: Instant,
    public val entries: List<EconomyTransactionEntryView>,
)

public sealed interface EconomyBalanceOutcome {

    public data class Available(public val view: EconomyAccountView) : EconomyBalanceOutcome

    public data object Disabled : EconomyBalanceOutcome

    public data object NotAuthorized : EconomyBalanceOutcome

    public data object StorageUnavailable : EconomyBalanceOutcome

}

public class EconomyRankingEntryView(
    public val rank: Int,
    public val playerId: UUID,
    public val balance: EconomyAmount,
)

public sealed interface EconomyRankingOutcome {

    public data class Available(
        public val entries: List<EconomyRankingEntryView>,
        public val page: Int,
        public val pageSize: Int,
        public val total: Int,
    ) : EconomyRankingOutcome

    public data object Disabled : EconomyRankingOutcome

    public data object NotAuthorized : EconomyRankingOutcome

    public data object StorageUnavailable : EconomyRankingOutcome

}

public class EconomyHistoryEntryView(
    public val transactionId: UUID,
    public val kind: EconomyTransactionKind,
    public val delta: EconomyAmount,
    public val balanceAfter: EconomyAmount,
    public val occurredAt: Instant,
)

public sealed interface EconomyHistoryOutcome {

    public data class Available(
        public val entries: List<EconomyHistoryEntryView>,
        public val page: Int,
        public val pageSize: Int,
        public val total: Int,
    ) : EconomyHistoryOutcome

    public data object Disabled : EconomyHistoryOutcome

    public data object NotAuthorized : EconomyHistoryOutcome

    public data object StorageUnavailable : EconomyHistoryOutcome

}

public sealed interface EconomyTransactionOutcome {

    public class Committed(public val receipt: EconomyTransactionReceiptView) :
        EconomyTransactionOutcome

    public class AlreadyCommitted(public val receipt: EconomyTransactionReceiptView) :
        EconomyTransactionOutcome

    public class NoChange(
        public val transactionId: UUID,
        public val balance: EconomyAmount,
    ) : EconomyTransactionOutcome

    public data object InsufficientFunds : EconomyTransactionOutcome

    public data object ExceedsBalanceLimit : EconomyTransactionOutcome

    public data object InvalidAmount : EconomyTransactionOutcome

    public data object SameAccount : EconomyTransactionOutcome

    public data object IdempotencyConflict : EconomyTransactionOutcome

    public data object NotAuthorized : EconomyTransactionOutcome

    public data object Disabled : EconomyTransactionOutcome

    public data object StorageUnavailable : EconomyTransactionOutcome

}

/** A trusted addon asking to pay on a player's behalf. */
public class PlayerEconomyTransferRequest(
    public val access: AddonAccess,
    public val payerId: UUID,
    public val receiverId: UUID,
    public val amount: EconomyAmount,
    public val requestKey: String,
    public val referenceId: String? = null,
)

/** A trusted addon issuing currency to a player with a durable [requestKey]. */
public class AddonEconomyGrantRequest(
    public val access: AddonAccess,
    public val playerId: UUID,
    public val amount: EconomyAmount,
    public val requestKey: String,
    public val reasonCode: String,
    public val referenceId: String? = null,
)

/** A trusted addon reclaiming currency from a player. */
public class AddonEconomyTakeRequest(
    public val access: AddonAccess,
    public val playerId: UUID,
    public val amount: EconomyAmount,
    public val requestKey: String,
    public val reasonCode: String,
    public val referenceId: String? = null,
)

/** An administrator-level addon setting a player's balance to an exact target. */
public class AddonEconomySetRequest(
    public val access: AddonAccess,
    public val playerId: UUID,
    public val targetBalance: EconomyAmount,
    public val requestKey: String,
    public val reasonCode: String,
    public val referenceId: String? = null,
)

public class EconomyHistoryQuery(
    public val playerId: UUID,
    public val page: Int = 1,
)

/** A new committed transaction fact; [affectedPlayerIds] is the minimal player projection. */
public class EconomyTransactionCommitted(
    public val transactionId: UUID,
    public val kind: EconomyTransactionKind,
    public val currencyId: String,
    public val occurredAt: Instant,
    public val affectedPlayerIds: List<UUID>,
)

public class EconomyBalanceChanged(
    public val playerId: UUID,
    public val transactionId: UUID,
    public val occurredAt: Instant,
)

public class EconomyAccountCreated(
    public val playerId: UUID,
    public val occurredAt: Instant,
)

/** One item's configured unit worth changed at a committed write point. */
public class EconomyWorthChanged(
    public val itemId: String,
    public val unitPrice: EconomyAmount,
    public val occurredAt: Instant,
)

public interface EconomyEvents {

    public fun onTransactionCommitted(
        listener: ApiEventListener<EconomyTransactionCommitted>,
    ): Subscription

    public fun onBalanceChanged(
        listener: ApiEventListener<EconomyBalanceChanged>,
    ): Subscription

    public fun onAccountCreated(
        listener: ApiEventListener<EconomyAccountCreated>,
    ): Subscription

    public fun onWorthChanged(
        listener: ApiEventListener<EconomyWorthChanged>,
    ): Subscription

}

/**
 * The economy surface. Only committed facts are observable; a rejected operation never produces an
 * event. System-level operations require an [AddonAccess] whose capability was explicitly granted in
 * the server configuration.
 */
public interface EconomyApi {

    public fun currency(): ApiQuery<EconomyCurrency>

    public fun balance(
        access: AddonAccess,
        playerId: UUID,
        callback: ApiCallback<EconomyBalanceOutcome>,
    ): ApiRequestHandle

    public fun balanceTop(
        access: AddonAccess,
        callback: ApiCallback<EconomyRankingOutcome>,
    ): ApiRequestHandle

    public fun history(
        access: AddonAccess,
        query: EconomyHistoryQuery,
        callback: ApiCallback<EconomyHistoryOutcome>,
    ): ApiRequestHandle

    public fun transferAsPlayer(
        request: PlayerEconomyTransferRequest,
        callback: ApiCallback<EconomyTransactionOutcome>,
    ): ApiRequestHandle

    public fun grantByAddon(
        request: AddonEconomyGrantRequest,
        callback: ApiCallback<EconomyTransactionOutcome>,
    ): ApiRequestHandle

    public fun takeByAddon(
        request: AddonEconomyTakeRequest,
        callback: ApiCallback<EconomyTransactionOutcome>,
    ): ApiRequestHandle

    public fun setByAddon(
        request: AddonEconomySetRequest,
        callback: ApiCallback<EconomyTransactionOutcome>,
    ): ApiRequestHandle

    public val worth: EconomyWorthApi
    public val events: EconomyEvents

}
