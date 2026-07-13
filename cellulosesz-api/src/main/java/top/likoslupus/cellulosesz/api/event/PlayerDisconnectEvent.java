package top.likoslupus.cellulosesz.api.event;

import top.likoslupus.cellulosesz.api.platform.CellPlayer;

import static java.util.Objects.requireNonNull;

public record PlayerDisconnectEvent(
        CellPlayer player
) {

    public PlayerDisconnectEvent {
        requireNonNull(player, "player");
    }

}
