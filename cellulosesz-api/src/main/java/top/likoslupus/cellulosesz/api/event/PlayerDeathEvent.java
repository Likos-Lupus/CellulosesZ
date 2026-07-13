package top.likoslupus.cellulosesz.api.event;

import top.likoslupus.cellulosesz.api.platform.CellPlayer;
import top.likoslupus.cellulosesz.api.teleport.CellLocation;

import static java.util.Objects.requireNonNull;

public record PlayerDeathEvent(
        CellPlayer player,
        CellLocation location,
        String source
) {

    public PlayerDeathEvent {
        requireNonNull(player, "player");
        requireNonNull(location, "location");
        requireNonNull(source, "source");
    }

}
