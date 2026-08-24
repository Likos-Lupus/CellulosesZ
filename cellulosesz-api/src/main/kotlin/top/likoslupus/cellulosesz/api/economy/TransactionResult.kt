package top.likoslupus.cellulosesz.api.economy

import top.likoslupus.cellulosesz.api.text.LocalizedMessage
import top.likoslupus.cellulosesz.api.text.MessageArguments
import java.math.BigDecimal

@JvmRecord
public data class TransactionResult(
    public val success: Boolean,
    public val message: LocalizedMessage,
    public val amount: BigDecimal,
    public val balance: BigDecimal
) {

    public companion object {

        @JvmStatic
        public fun success(
            key: String,
            amount: BigDecimal,
            balance: BigDecimal
        ): TransactionResult =
            TransactionResult(
                true,
                LocalizedMessage.of(key),
                amount,
                balance
            )

        @JvmStatic
        public fun success(
            key: String,
            arguments: MessageArguments,
            amount: BigDecimal,
            balance: BigDecimal
        ): TransactionResult =
            TransactionResult(
                true,
                LocalizedMessage.of(key, arguments),
                amount,
                balance
            )

        @JvmStatic
        public fun failure(
            key: String,
            amount: BigDecimal,
            balance: BigDecimal
        ): TransactionResult =
            TransactionResult(
                false,
                LocalizedMessage.of(key),
                amount,
                balance
            )

        @JvmStatic
        public fun failure(
            key: String,
            arguments: MessageArguments,
            amount: BigDecimal,
            balance: BigDecimal
        ): TransactionResult =
            TransactionResult(
                false,
                LocalizedMessage.of(key, arguments),
                amount,
                balance
            )

    }

}
