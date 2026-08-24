package top.likoslupus.cellulosesz.api.event

import top.likoslupus.cellulosesz.api.platform.CellPlayer

public class PlayerPickupEvent(
    private val player: CellPlayer,
    private val itemId: String,
    private val count: Int
) : AbstractCancellableEvent() {

    public fun player(): CellPlayer = player

    public fun itemId(): String = itemId

    public fun count(): Int = count

}
