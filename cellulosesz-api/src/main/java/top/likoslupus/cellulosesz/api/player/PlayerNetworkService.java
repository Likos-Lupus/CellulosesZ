package top.likoslupus.cellulosesz.api.player;

import top.likoslupus.cellulosesz.api.platform.CellPlayer;

import java.net.InetAddress;
import java.util.Optional;

/**
 * Exposes only the current connection address and never performs DNS resolution.
 */
public interface PlayerNetworkService {

    Optional<InetAddress> address(CellPlayer player);

}
