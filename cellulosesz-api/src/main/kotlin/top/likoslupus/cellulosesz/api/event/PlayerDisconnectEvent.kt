package top.likoslupus.cellulosesz.api.event

import top.likoslupus.cellulosesz.api.platform.CellPlayer
import top.likoslupus.cellulosesz.api.teleport.CellLocation

@JvmRecord
public data class PlayerDisconnectEvent(
    public val player: CellPlayer,
    public val location: CellLocation
)
