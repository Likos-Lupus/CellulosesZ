package top.likoslupus.cellulosesz.api.economy;

import org.jspecify.annotations.Nullable;

import java.math.BigDecimal;
import java.util.Collection;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;

public interface EconomyService {

    BigDecimal balance(UUID uuid);

    default String format(BigDecimal amount) {
        return amount.toPlainString();
    }

    CompletableFuture<TransactionResult> deposit(
            UUID uuid,
            BigDecimal amount,
            TransactionCause cause
    );

    CompletableFuture<TransactionResult> withdraw(
            UUID uuid,
            BigDecimal amount,
            TransactionCause cause
    );

    CompletableFuture<TransactionResult> setBalance(
            UUID uuid,
            BigDecimal amount,
            TransactionCause cause
    );

    CompletableFuture<TransactionResult> transfer(
            UUID from,
            UUID to,
            BigDecimal amount,
            TransactionCause cause
    );

    CompletableFuture<TransactionResult> transferMany(
            UUID from,
            Collection<UUID> recipients,
            BigDecimal amountEach,
            TransactionCause cause
    );

    default List<BalanceEntry> topBalances(int limit) {
        return topBalances(limit, null, null);
    }

    List<BalanceEntry> topBalances(
            int limit,
            @Nullable BigDecimal minimum,
            @Nullable BigDecimal maximum
    );

}
