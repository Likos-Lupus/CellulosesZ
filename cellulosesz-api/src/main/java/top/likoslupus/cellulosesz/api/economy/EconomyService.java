package top.likoslupus.cellulosesz.api.economy;

import java.math.BigDecimal;
import java.util.Collection;
import java.util.List;
import java.util.UUID;

public interface EconomyService {

    BigDecimal balance(UUID uuid);

    TransactionResult deposit(
            UUID uuid,
            BigDecimal amount,
            TransactionCause cause
    );

    TransactionResult withdraw(
            UUID uuid,
            BigDecimal amount,
            TransactionCause cause
    );

    TransactionResult setBalance(
            UUID uuid,
            BigDecimal amount,
            TransactionCause cause
    );

    TransactionResult transfer(
            UUID from,
            UUID to,
            BigDecimal amount,
            TransactionCause cause
    );

    TransactionResult transferMany(
            UUID from,
            Collection<UUID> recipients,
            BigDecimal amountEach,
            TransactionCause cause
    );

    List<BalanceEntry> topBalances(int limit);

}
