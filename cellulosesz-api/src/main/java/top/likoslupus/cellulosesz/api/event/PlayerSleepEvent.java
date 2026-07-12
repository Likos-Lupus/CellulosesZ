package top.likoslupus.cellulosesz.api.event;

import top.likoslupus.cellulosesz.api.platform.CellPlayer;
import top.likoslupus.cellulosesz.api.teleport.CellLocation;

import java.util.Objects;

public final class PlayerSleepEvent extends AbstractCancellableEvent {

    private final CellPlayer player;
    private final CellLocation bed;
    private final Action action;

    public PlayerSleepEvent(
            CellPlayer player,
            CellLocation bed,
            Action action
    ) {
        this.player = Objects.requireNonNull(player, "player");
        this.bed = Objects.requireNonNull(bed, "bed");
        this.action = Objects.requireNonNull(action, "action");
    }

    public CellPlayer player() {
        return player;
    }

    public CellLocation bed() {
        return bed;
    }

    public Action action() {
        return action;
    }

    public enum Action {

        START,
        STOP

    }

}
