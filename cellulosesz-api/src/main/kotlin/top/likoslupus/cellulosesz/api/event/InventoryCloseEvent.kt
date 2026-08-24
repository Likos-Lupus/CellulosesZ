package top.likoslupus.cellulosesz.api.event

import top.likoslupus.cellulosesz.api.platform.CellPlayer

@JvmRecord
public data class InventoryCloseEvent(
    public val player: CellPlayer,
    public val inventoryType: String
)
