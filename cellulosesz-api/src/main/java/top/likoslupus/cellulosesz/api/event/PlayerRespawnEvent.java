package top.likoslupus.cellulosesz.api.event;

import top.likoslupus.cellulosesz.api.platform.CellPlayer;
import top.likoslupus.cellulosesz.api.teleport.CellLocation;

import java.util.Objects;

public final class PlayerRespawnEvent {

    private final CellPlayer player;
    private final boolean alive;
    private CellLocation location;

    public PlayerRespawnEvent(
            CellPlayer player,
            CellLocation location,
            boolean alive
    ) {
        this.player = Objects.requireNonNull(player, "player");
        this.location = Objects.requireNonNull(location, "location");
        this.alive = alive;
    }

    public CellPlayer player() {
        return player;
    }

    public CellLocation location() {
        return location;
    }

    public void location(CellLocation location) {
        this.location = Objects.requireNonNull(location, "location");
    }

    public boolean alive() {
        return alive;
    }

}
