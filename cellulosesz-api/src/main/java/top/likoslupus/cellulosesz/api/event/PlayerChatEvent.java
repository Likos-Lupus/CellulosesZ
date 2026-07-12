package top.likoslupus.cellulosesz.api.event;

import top.likoslupus.cellulosesz.api.platform.CellPlayer;

import java.util.Objects;

public final class PlayerChatEvent extends AbstractCancellableEvent {

    private final CellPlayer player;
    private String message;

    public PlayerChatEvent(
            CellPlayer player,
            String message
    ) {
        this.player = Objects.requireNonNull(player, "player");
        this.message = Objects.requireNonNull(message, "message");
    }

    public CellPlayer player() {
        return player;
    }

    public String message() {
        return message;
    }

    public void message(String message) {
        this.message = Objects.requireNonNull(message, "message");
    }

}
