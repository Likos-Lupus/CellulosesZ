package top.likoslupus.cellulosesz.api.event

import top.likoslupus.cellulosesz.api.platform.CellPlayer

@JvmRecord
public data class PlayerWorldChangeEvent(
    public val player: CellPlayer,
    public val fromWorld: String,
    public val toWorld: String
)
