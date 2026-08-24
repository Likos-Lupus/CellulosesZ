package top.likoslupus.cellulosesz.api.event

import top.likoslupus.cellulosesz.api.platform.CellPlayer
import top.likoslupus.cellulosesz.api.teleport.CellLocation

public class PlayerSleepEvent(
    private val player: CellPlayer,
    private val bed: CellLocation,
    private val action: Action
) : AbstractCancellableEvent() {

    public fun player(): CellPlayer = player

    public fun bed(): CellLocation = bed

    public fun action(): Action = action

    public enum class Action {

        START,
        STOP

    }

}
