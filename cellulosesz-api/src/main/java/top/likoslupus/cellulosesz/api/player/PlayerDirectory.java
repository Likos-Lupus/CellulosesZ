package top.likoslupus.cellulosesz.api.player;

import top.likoslupus.cellulosesz.api.platform.CellPlayer;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Server-thread online-player snapshots.
 */
public interface PlayerDirectory {

    List<CellPlayer> onlinePlayers();

    Optional<CellPlayer> onlinePlayer(UUID uuid);

    Optional<CellPlayer> onlinePlayer(String name);

    List<String> onlinePlayerNames();

}
