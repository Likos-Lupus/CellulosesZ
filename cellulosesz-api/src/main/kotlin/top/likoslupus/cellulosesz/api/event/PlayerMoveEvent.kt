package top.likoslupus.cellulosesz.api.event

import top.likoslupus.cellulosesz.api.platform.CellPlayer
import top.likoslupus.cellulosesz.api.teleport.CellLocation
import kotlin.math.floor

public class PlayerMoveEvent(
    private val player: CellPlayer,
    private val from: CellLocation,
    private var to: CellLocation
) : AbstractCancellableEvent() {

    public fun player(): CellPlayer = player

    public fun from(): CellLocation = from

    public fun to(): CellLocation = to

    public fun to(to: CellLocation) {
        this.to = to
    }

    public fun changedBlock(): Boolean =
        from.world != to.world ||
                floor(from.x).toInt() != floor(to.x).toInt() ||
                floor(from.y).toInt() != floor(to.y).toInt() ||
                floor(from.z).toInt() != floor(to.z).toInt()

}
