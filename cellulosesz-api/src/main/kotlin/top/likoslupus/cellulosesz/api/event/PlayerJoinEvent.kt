package top.likoslupus.cellulosesz.api.event

import top.likoslupus.cellulosesz.api.platform.CellPlayer

@JvmRecord
public data class PlayerJoinEvent(
    public val player: CellPlayer
)
