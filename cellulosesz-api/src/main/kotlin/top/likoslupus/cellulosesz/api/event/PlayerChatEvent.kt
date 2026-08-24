package top.likoslupus.cellulosesz.api.event

import top.likoslupus.cellulosesz.api.platform.CellPlayer

public class PlayerChatEvent(
    private val player: CellPlayer,
    private var message: String
) : AbstractCancellableEvent() {

    public fun player(): CellPlayer = player

    public fun message(): String = message

    public fun message(message: String) {
        this.message = message
    }

}
