package top.likoslupus.cellulosesz.api.player

import top.likoslupus.cellulosesz.api.platform.CellPlayer
import java.util.*

/**
 * Server-thread online-player snapshots.
 */
public interface PlayerDirectory {

    public fun onlinePlayers(): List<CellPlayer>

    public fun onlinePlayer(uuid: UUID): CellPlayer?

    public fun onlinePlayer(name: String): CellPlayer?

    public fun onlinePlayerNames(): List<String>

}
