package top.likoslupus.cellulosesz.api.player

import top.likoslupus.cellulosesz.api.platform.CellPlayer
import java.net.InetAddress

/**
 * Exposes only the current connection address and never performs DNS resolution.
 */
public interface PlayerNetworkService {

    public fun address(player: CellPlayer): InetAddress?

}
