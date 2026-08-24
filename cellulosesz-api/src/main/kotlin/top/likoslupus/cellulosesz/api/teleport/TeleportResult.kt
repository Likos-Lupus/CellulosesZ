package top.likoslupus.cellulosesz.api.teleport

import top.likoslupus.cellulosesz.api.text.LocalizedMessage
import top.likoslupus.cellulosesz.api.text.MessageArguments

@JvmRecord
public data class TeleportResult(
    public val status: TeleportStatus,
    public val destination: CellLocation?,
    public val message: LocalizedMessage
) {

    init {
        require(!(status == TeleportStatus.SUCCESS && destination == null)) {
            "Successful teleport must expose its destination"
        }
    }

    public fun success(): Boolean = status == TeleportStatus.SUCCESS

    public companion object {

        @JvmStatic
        public fun success(location: CellLocation): TeleportResult =
            TeleportResult(
                TeleportStatus.SUCCESS,
                location,
                LocalizedMessage.of("service.teleport.success")
            )

        @JvmStatic
        public fun failed(status: TeleportStatus, key: String): TeleportResult {
            require(status != TeleportStatus.SUCCESS) {
                "failure status required"
            }
            return TeleportResult(status, null, LocalizedMessage.of(key))
        }

        @JvmStatic
        public fun failed(
            status: TeleportStatus,
            key: String,
            arguments: MessageArguments
        ): TeleportResult {
            require(status != TeleportStatus.SUCCESS) {
                "failure status required"
            }
            return TeleportResult(status, null, LocalizedMessage.of(key, arguments))
        }

    }

}
