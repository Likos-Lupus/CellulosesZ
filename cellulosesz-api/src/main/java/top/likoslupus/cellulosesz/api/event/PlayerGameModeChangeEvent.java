package top.likoslupus.cellulosesz.api.event;

import top.likoslupus.cellulosesz.api.platform.CellPlayer;

import java.util.Objects;

public final class PlayerGameModeChangeEvent extends AbstractCancellableEvent {

    private final CellPlayer player;
    private final String fromGameMode;
    private final String toGameMode;

    public PlayerGameModeChangeEvent(
            CellPlayer player,
            String fromGameMode,
            String toGameMode
    ) {
        this.player = Objects.requireNonNull(player, "player");
        this.fromGameMode = Objects.requireNonNull(fromGameMode, "fromGameMode");
        this.toGameMode = Objects.requireNonNull(toGameMode, "toGameMode");
    }

    public CellPlayer player() {
        return player;
    }

    public String fromGameMode() {
        return fromGameMode;
    }

    public String toGameMode() {
        return toGameMode;
    }

}
