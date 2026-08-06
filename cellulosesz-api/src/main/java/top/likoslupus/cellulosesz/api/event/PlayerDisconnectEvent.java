package top.likoslupus.cellulosesz.api.event;

import top.likoslupus.cellulosesz.api.platform.CellPlayer;
import top.likoslupus.cellulosesz.api.teleport.CellLocation;

import static java.util.Objects.requireNonNull;

public record PlayerDisconnectEvent(
        CellPlayer player,
        CellLocation location
) {

    public PlayerDisconnectEvent {
        requireNonNull(player, "player");
        requireNonNull(location, "location");
    }

}
