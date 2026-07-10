package top.likoslupus.cellulosesz.api.economy;

import java.math.BigDecimal;

public record TransactionResult(
        boolean success,
        String message,
        BigDecimal amount,
        BigDecimal balance
) {

    public static TransactionResult success(
            String message,
            BigDecimal amount,
            BigDecimal balance
    ) {
        return new TransactionResult(true, message, amount, balance);
    }

    public static TransactionResult failure(
            String message,
            BigDecimal amount,
            BigDecimal balance
    ) {
        return new TransactionResult(false, message, amount, balance);
    }

}
