package top.likoslupus.cellulosesz.api.player

import top.likoslupus.cellulosesz.api.platform.CellPlayer
import java.util.*

@JvmRecord
public data class ResolvedPlayer(
    public val state: ResolvedPlayerState,
    public val uuid: UUID?,
    public val name: String,
    public val onlinePlayer: CellPlayer?,
    public val vanished: Boolean
)
