package top.likoslupus.cellulosesz.api.event

import top.likoslupus.cellulosesz.api.platform.CellPlayer
import java.util.*

public class PlayerDamageEvent(
    private val player: CellPlayer,
    private val source: String,
    private val attacker: UUID?,
    private val amount: Float
) : AbstractCancellableEvent() {

    public fun player(): CellPlayer = player

    public fun source(): String = source

    public fun attacker(): UUID? = attacker

    public fun amount(): Float = amount

}
