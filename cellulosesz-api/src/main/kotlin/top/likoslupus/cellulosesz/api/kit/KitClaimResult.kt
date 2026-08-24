package top.likoslupus.cellulosesz.api.kit

import top.likoslupus.cellulosesz.api.text.LocalizedMessage
import top.likoslupus.cellulosesz.api.text.MessageArguments

@JvmRecord
public data class KitClaimResult(
    public val success: Boolean,
    public val message: LocalizedMessage
) {

    public companion object {

        @JvmStatic
        public fun success(message: LocalizedMessage): KitClaimResult =
            KitClaimResult(true, message)

        @JvmStatic
        public fun success(key: String): KitClaimResult =
            KitClaimResult(true, LocalizedMessage.of(key))

        @JvmStatic
        public fun success(key: String, arguments: MessageArguments): KitClaimResult =
            KitClaimResult(true, LocalizedMessage.of(key, arguments))

        @JvmStatic
        public fun failure(message: LocalizedMessage): KitClaimResult =
            KitClaimResult(false, message)

        @JvmStatic
        public fun failure(key: String): KitClaimResult =
            KitClaimResult(false, LocalizedMessage.of(key))

        @JvmStatic
        public fun failure(key: String, arguments: MessageArguments): KitClaimResult =
            KitClaimResult(false, LocalizedMessage.of(key, arguments))

    }

}
