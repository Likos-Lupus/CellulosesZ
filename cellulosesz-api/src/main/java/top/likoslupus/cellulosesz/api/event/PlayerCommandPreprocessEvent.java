package top.likoslupus.cellulosesz.api.event;

import top.likoslupus.cellulosesz.api.platform.CellPlayer;

import java.util.Objects;

public final class PlayerCommandPreprocessEvent extends AbstractCancellableEvent {

    private final CellPlayer player;
    private String command;

    public PlayerCommandPreprocessEvent(
            CellPlayer player,
            String command
    ) {
        this.player = Objects.requireNonNull(player, "player");
        this.command = Objects.requireNonNull(command, "command");
    }

    public CellPlayer player() {
        return player;
    }

    public String command() {
        return command;
    }

    public void command(String command) {
        this.command = Objects.requireNonNull(command, "command");
    }

}
