package top.likoslupus.cellulosesz.api.event

import top.likoslupus.cellulosesz.api.platform.CellPlayer
import top.likoslupus.cellulosesz.api.teleport.CellLocation
import top.likoslupus.cellulosesz.api.util.toImmutableList

public class SignEditEvent(
    private val player: CellPlayer,
    private val location: CellLocation,
    private val front: Boolean,
    previousLines: List<String>,
    lines: List<String>
) : AbstractCancellableEvent() {

    private val previousLines: List<String> = previousLines.toImmutableList()
    private var lines: List<String> = emptyList()

    init {
        lines(lines)
    }

    public fun lines(lines: List<String>) {
        this.lines = lines.toImmutableList()
    }

    public fun player(): CellPlayer = player

    public fun location(): CellLocation = location

    public fun front(): Boolean = front

    public fun previousLines(): List<String> = previousLines

    public fun lines(): List<String> = lines

}
