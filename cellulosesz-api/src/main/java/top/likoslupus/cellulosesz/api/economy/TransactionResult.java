package top.likoslupus.cellulosesz.api.economy;

import top.likoslupus.cellulosesz.api.text.LocalizedMessage;

import java.math.BigDecimal;
import java.util.Map;

public record TransactionResult(
        boolean success,
        LocalizedMessage message,
        BigDecimal amount,
        BigDecimal balance
) {

    public static TransactionResult success(
            String key,
            BigDecimal amount,
            BigDecimal balance
    ) {
        return new TransactionResult(true, LocalizedMessage.of(key), amount, balance);
    }

    public static TransactionResult success(
            String key,
            Map<String, ?> placeholders,
            BigDecimal amount,
            BigDecimal balance
    ) {
        return new TransactionResult(true, LocalizedMessage.of(key, placeholders), amount, balance);
    }

    public static TransactionResult failure(
            String key,
            BigDecimal amount,
            BigDecimal balance
    ) {
        return new TransactionResult(false, LocalizedMessage.of(key), amount, balance);
    }

    public static TransactionResult failure(
            String key,
            Map<String, ?> placeholders,
            BigDecimal amount,
            BigDecimal balance
    ) {
        return new TransactionResult(false, LocalizedMessage.of(key, placeholders), amount, balance);
    }

}
