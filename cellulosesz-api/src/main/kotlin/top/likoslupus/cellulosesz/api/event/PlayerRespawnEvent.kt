package top.likoslupus.cellulosesz.api.event

import top.likoslupus.cellulosesz.api.platform.CellPlayer
import top.likoslupus.cellulosesz.api.teleport.CellLocation

public class PlayerRespawnEvent(
    private val player: CellPlayer,
    private var location: CellLocation,
    private val alive: Boolean
) {

    public fun player(): CellPlayer = player

    public fun location(): CellLocation = location

    public fun location(location: CellLocation) {
        this.location = location
    }

    public fun alive(): Boolean = alive

}
