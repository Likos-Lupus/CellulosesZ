package top.likoslupus.cellulosesz.api.event;

import top.likoslupus.cellulosesz.api.platform.CellPlayer;

import static java.util.Objects.requireNonNull;

public record PlayerWorldChangeEvent(
        CellPlayer player,
        String fromWorld,
        String toWorld
) {

    public PlayerWorldChangeEvent {
        requireNonNull(player, "player");
        requireNonNull(fromWorld, "fromWorld");
        requireNonNull(toWorld, "toWorld");
    }

}
