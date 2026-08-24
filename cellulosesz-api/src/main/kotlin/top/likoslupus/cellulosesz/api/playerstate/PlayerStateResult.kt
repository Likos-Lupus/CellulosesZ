package top.likoslupus.cellulosesz.api.playerstate

import top.likoslupus.cellulosesz.api.text.LocalizedMessage
import top.likoslupus.cellulosesz.api.text.MessageArguments

@JvmRecord
public data class PlayerStateResult(
    public val success: Boolean,
    public val message: LocalizedMessage
) {

    public companion object {

        @JvmStatic
        public fun success(message: LocalizedMessage): PlayerStateResult =
            PlayerStateResult(true, message)

        @JvmStatic
        public fun success(key: String): PlayerStateResult =
            PlayerStateResult(true, LocalizedMessage.of(key))

        @JvmStatic
        public fun success(key: String, arguments: MessageArguments): PlayerStateResult =
            PlayerStateResult(true, LocalizedMessage.of(key, arguments))

        @JvmStatic
        public fun failure(message: LocalizedMessage): PlayerStateResult =
            PlayerStateResult(false, message)

        @JvmStatic
        public fun failure(key: String): PlayerStateResult =
            PlayerStateResult(false, LocalizedMessage.of(key))

        @JvmStatic
        public fun failure(key: String, arguments: MessageArguments): PlayerStateResult =
            PlayerStateResult(false, LocalizedMessage.of(key, arguments))

    }

}
