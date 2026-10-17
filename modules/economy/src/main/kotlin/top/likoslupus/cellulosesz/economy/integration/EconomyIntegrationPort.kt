package top.likoslupus.cellulosesz.economy.integration

import java.math.BigDecimal
import java.time.Instant
import java.util.*

/** Who initiated an operation; an attribution label, never a bearer token. */
public enum class EconomyActorKind {

    PLAYER,
    ADMIN,
    ADDON,
    INTERNAL,

}

public class EconomyActorDescriptor(
    public val kind: EconomyActorKind,
    public val id: String,
)

public class EconomyAccountSnapshot(
    public val playerId: UUID,
    public val amount: BigDecimal,
    public val accountExists: Boolean,
)

public sealed interface EconomyBalanceResult {

    public data class Available(public val snapshot: EconomyAccountSnapshot) : EconomyBalanceResult

    public data object Disabled : EconomyBalanceResult

    public data object StorageUnavailable : EconomyBalanceResult

}

public enum class EconomyTransactionKind {

    OPENING,
    TRANSFER,
    GRANT,
    TAKE,
    SET,
    SET_NO_CHANGE,
    COMPENSATION,

}

/** A player's signed change in one committed transaction. */
public class EconomyEntryView(
    public val playerId: UUID,
    public val delta: BigDecimal,
    public val balanceAfter: BigDecimal,
)

public class EconomyTransactionReceipt(
    public val transactionId: UUID,
    public val kind: EconomyTransactionKind,
    public val occurredAt: Instant,
    public val entries: List<EconomyEntryView>,
)

public sealed interface EconomyTransactionResult {

    public class Committed(public val receipt: EconomyTransactionReceipt) : EconomyTransactionResult

    public class AlreadyCommitted(public val receipt: EconomyTransactionReceipt) :
        EconomyTransactionResult

    public class NoChange(
        public val transactionId: UUID,
        public val balance: BigDecimal,
    ) : EconomyTransactionResult

    public data object InsufficientFunds : EconomyTransactionResult

    public data object ExceedsBalanceLimit : EconomyTransactionResult

    public data object InvalidAmount : EconomyTransactionResult

    public data object TransferLimitExceeded : EconomyTransactionResult

    public data object SameAccount : EconomyTransactionResult

    public data object IdempotencyConflict : EconomyTransactionResult

    public data object Disabled : EconomyTransactionResult

    public data object StorageUnavailable : EconomyTransactionResult

}

public class EconomyTransferRequest(
    public val actor: EconomyActorDescriptor,
    public val payerId: UUID,
    public val receiverId: UUID,
    public val amount: BigDecimal,
    public val originId: String,
    public val requestKey: String,
    public val reasonCode: String,
    public val referenceId: String? = null,
)

public class EconomyGrantRequest(
    public val actor: EconomyActorDescriptor,
    public val playerId: UUID,
    public val amount: BigDecimal,
    public val originId: String,
    public val requestKey: String,
    public val reasonCode: String,
    public val referenceId: String? = null,
)

public class EconomyTakeRequest(
    public val actor: EconomyActorDescriptor,
    public val playerId: UUID,
    public val amount: BigDecimal,
    public val originId: String,
    public val requestKey: String,
    public val reasonCode: String,
    public val referenceId: String? = null,
)

public class EconomySetRequest(
    public val actor: EconomyActorDescriptor,
    public val playerId: UUID,
    public val target: BigDecimal,
    public val originId: String,
    public val requestKey: String,
    public val reasonCode: String,
    public val referenceId: String? = null,
)

public class EconomyHistoryRow(
    public val transactionId: UUID,
    public val kind: EconomyTransactionKind,
    public val delta: BigDecimal,
    public val balanceAfter: BigDecimal,
    public val at: Instant,
    public val reasonCode: String,
    public val referenceId: String?,
)

public sealed interface EconomyHistoryResult {

    public data class Available(
        public val rows: List<EconomyHistoryRow>,
        public val total: Int,
        public val page: Int,
        public val pageSize: Int,
    ) : EconomyHistoryResult

    public data object Disabled : EconomyHistoryResult

    public data object StorageUnavailable : EconomyHistoryResult

}

public class EconomyRankingRow(
    public val playerId: UUID,
    public val amount: BigDecimal,
)

public sealed interface EconomyRankingResult {

    public data class Available(
        public val rows: List<EconomyRankingRow>,
        public val total: Int,
        public val page: Int,
        public val pageSize: Int,
    ) : EconomyRankingResult

    public data object Disabled : EconomyRankingResult

    public data object StorageUnavailable : EconomyRankingResult

}

public class EconomyBalanceChange(
    public val playerId: UUID,
    public val delta: BigDecimal,
)

public class EconomyCommittedEvent(
    public val transactionId: UUID,
    public val kind: EconomyTransactionKind,
    public val currencyId: String,
    public val occurredAt: Instant,
    public val changes: List<EconomyBalanceChange>,
)

/**
 * Reads new committed economy facts after the fact. Every callback is best effort and must never
 * reverse an already-committed change.
 */
public interface EconomyDomainObserver {

    public fun onTransactionCommitted(event: EconomyCommittedEvent)

    public fun onAccountOpened(playerId: UUID, at: Instant)

    public companion object {

        public val None: EconomyDomainObserver = object : EconomyDomainObserver {

            override fun onTransactionCommitted(event: EconomyCommittedEvent) = Unit

            override fun onAccountOpened(playerId: UUID, at: Instant) = Unit

        }

    }

}

/**
 * The narrow economy surface the composition root (and the public API adapter) depends on. Every
 * mutation funnels through one domain transaction; this port never exposes JDBC, a connection or a
 * mutable Minecraft object.
 */
public interface EconomyIntegrationPort {

    public suspend fun balance(playerId: UUID): EconomyBalanceResult

    public suspend fun ensureAccount(playerId: UUID): Boolean

    public suspend fun transfer(intent: EconomyTransferRequest): EconomyTransactionResult

    public suspend fun grant(intent: EconomyGrantRequest): EconomyTransactionResult

    public suspend fun take(intent: EconomyTakeRequest): EconomyTransactionResult

    public suspend fun set(intent: EconomySetRequest): EconomyTransactionResult

    public suspend fun history(playerId: UUID, page: Int): EconomyHistoryResult

    public suspend fun top(page: Int): EconomyRankingResult

}
