package top.likoslupus.cellulosesz.api.economy

import java.math.BigDecimal
import java.util.*
import java.util.concurrent.CompletableFuture

public interface EconomyService {

    public fun balance(uuid: UUID): BigDecimal

    public fun format(amount: BigDecimal): String = amount.toPlainString()

    public fun deposit(
        uuid: UUID,
        amount: BigDecimal,
        cause: TransactionCause
    ): CompletableFuture<TransactionResult>

    public fun withdraw(
        uuid: UUID,
        amount: BigDecimal,
        cause: TransactionCause
    ): CompletableFuture<TransactionResult>

    public fun setBalance(
        uuid: UUID,
        amount: BigDecimal,
        cause: TransactionCause
    ): CompletableFuture<TransactionResult>

    public fun transfer(
        from: UUID,
        to: UUID,
        amount: BigDecimal,
        cause: TransactionCause
    ): CompletableFuture<TransactionResult>

    public fun transferMany(
        from: UUID,
        recipients: Collection<UUID>,
        amountEach: BigDecimal,
        cause: TransactionCause
    ): CompletableFuture<TransactionResult>

    public fun topBalances(limit: Int): List<BalanceEntry> =
        topBalances(limit, BalanceFilter.all())

    public fun topBalances(
        limit: Int,
        filter: BalanceFilter
    ): List<BalanceEntry>

}
