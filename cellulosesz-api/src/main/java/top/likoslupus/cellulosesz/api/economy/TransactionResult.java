package top.likoslupus.cellulosesz.api.economy;

import top.likoslupus.cellulosesz.api.text.LocalizedMessage;
import top.likoslupus.cellulosesz.api.text.MessageArguments;

import java.math.BigDecimal;

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
        return new TransactionResult(
                true,
                LocalizedMessage.of(key),
                amount,
                balance
        );
    }

    public static TransactionResult success(
            String key,
            MessageArguments arguments,
            BigDecimal amount,
            BigDecimal balance
    ) {
        return new TransactionResult(
                true,
                LocalizedMessage.of(key, arguments),
                amount,
                balance
        );
    }

    public static TransactionResult failure(
            String key,
            BigDecimal amount,
            BigDecimal balance
    ) {
        return new TransactionResult(
                false,
                LocalizedMessage.of(key),
                amount,
                balance
        );
    }

    public static TransactionResult failure(
            String key,
            MessageArguments arguments,
            BigDecimal amount,
            BigDecimal balance
    ) {
        return new TransactionResult(
                false,
                LocalizedMessage.of(key, arguments),
                amount,
                balance
        );
    }

}
