package top.likoslupus.cellulosesz.api.world

import top.likoslupus.cellulosesz.api.text.LocalizedMessage
import top.likoslupus.cellulosesz.api.text.MessageArguments

@JvmRecord
public data class WorldResult(
    public val success: Boolean,
    public val message: LocalizedMessage
) {

    public companion object {

        @JvmStatic
        public fun success(message: LocalizedMessage): WorldResult =
            WorldResult(true, message)

        @JvmStatic
        public fun success(key: String): WorldResult =
            WorldResult(true, LocalizedMessage.of(key))

        @JvmStatic
        public fun success(key: String, arguments: MessageArguments): WorldResult =
            WorldResult(true, LocalizedMessage.of(key, arguments))

        @JvmStatic
        public fun failure(message: LocalizedMessage): WorldResult =
            WorldResult(false, message)

        @JvmStatic
        public fun failure(key: String): WorldResult =
            WorldResult(false, LocalizedMessage.of(key))

        @JvmStatic
        public fun failure(key: String, arguments: MessageArguments): WorldResult =
            WorldResult(false, LocalizedMessage.of(key, arguments))

    }

}
