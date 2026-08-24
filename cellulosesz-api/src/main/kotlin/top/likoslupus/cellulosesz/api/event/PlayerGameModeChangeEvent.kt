package top.likoslupus.cellulosesz.api.event

import top.likoslupus.cellulosesz.api.platform.CellPlayer

public class PlayerGameModeChangeEvent(
    private val player: CellPlayer,
    private val fromGameMode: String,
    private val toGameMode: String
) : AbstractCancellableEvent() {

    public fun player(): CellPlayer = player

    public fun fromGameMode(): String = fromGameMode

    public fun toGameMode(): String = toGameMode

}
