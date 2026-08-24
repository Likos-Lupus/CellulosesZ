package top.likoslupus.cellulosesz.api.event

import top.likoslupus.cellulosesz.api.platform.CellPlayer
import java.util.*

public class PlayerAttackEvent(
    private val player: CellPlayer,
    private val targetPlayer: UUID?,
    private val targetType: String
) : AbstractCancellableEvent() {

    public fun player(): CellPlayer = player

    public fun targetPlayer(): UUID? = targetPlayer

    public fun targetType(): String = targetType

}
