package top.likoslupus.cellulosesz.api.event

import top.likoslupus.cellulosesz.api.platform.CellPlayer

public class PlayerFishEvent(
    private val player: CellPlayer,
    private val action: Action,
    private val caughtType: String
) : AbstractCancellableEvent() {

    public fun player(): CellPlayer = player

    public fun action(): Action = action

    public fun caughtType(): String = caughtType

    public enum class Action {

        CAST,
        REEL_IN,
        CAUGHT_ITEM,
        CAUGHT_ENTITY,
        FAILED

    }

}
