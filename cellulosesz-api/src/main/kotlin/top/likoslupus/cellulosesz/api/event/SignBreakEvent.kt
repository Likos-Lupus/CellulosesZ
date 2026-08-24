package top.likoslupus.cellulosesz.api.event

import top.likoslupus.cellulosesz.api.platform.CellPlayer
import top.likoslupus.cellulosesz.api.teleport.CellLocation

public class SignBreakEvent(
    private val player: CellPlayer,
    private val location: CellLocation,
    private val frontLines: List<String>,
    private val backLines: List<String>
) : AbstractCancellableEvent() {

    public fun player(): CellPlayer = player

    public fun location(): CellLocation = location

    public fun frontLines(): List<String> = frontLines

    public fun backLines(): List<String> = backLines

}
