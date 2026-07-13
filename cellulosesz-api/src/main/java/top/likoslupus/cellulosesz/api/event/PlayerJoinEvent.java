package top.likoslupus.cellulosesz.api.event;

import top.likoslupus.cellulosesz.api.platform.CellPlayer;

import static java.util.Objects.requireNonNull;

public record PlayerJoinEvent(
        CellPlayer player
) {

    public PlayerJoinEvent {
        requireNonNull(player, "player");
    }

}
