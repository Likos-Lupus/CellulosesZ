package top.likoslupus.cellulosesz.api.event;

import top.likoslupus.cellulosesz.api.platform.CellPlayer;

import java.util.Objects;

public final class PlayerFishEvent extends AbstractCancellableEvent {

    private final CellPlayer player;
    private final Action action;
    private final String caughtType;

    public PlayerFishEvent(
            CellPlayer player,
            Action action,
            String caughtType
    ) {
        this.player = Objects.requireNonNull(player, "player");
        this.action = Objects.requireNonNull(action, "action");
        this.caughtType = Objects.requireNonNull(caughtType, "caughtType");
    }

    public CellPlayer player() {
        return player;
    }

    public Action action() {
        return action;
    }

    public String caughtType() {
        return caughtType;
    }

    public enum Action {

        CAST,
        REEL_IN,
        CAUGHT_ITEM,
        CAUGHT_ENTITY,
        FAILED

    }

}
