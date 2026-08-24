package top.likoslupus.cellulosesz.api.event

import top.likoslupus.cellulosesz.api.platform.CellPlayer

public class PlayerCommandPreprocessEvent(
    private val player: CellPlayer,
    private var command: String
) : AbstractCancellableEvent() {

    public fun player(): CellPlayer = player

    public fun command(): String = command

    public fun command(command: String) {
        this.command = command
    }

}
