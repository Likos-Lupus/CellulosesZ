package top.likoslupus.cellulosesz.api.event;

import top.likoslupus.cellulosesz.api.platform.CellPlayer;
import top.likoslupus.cellulosesz.api.teleport.CellLocation;

import java.util.Objects;

public final class PlayerMoveEvent extends AbstractCancellableEvent {

    private final CellPlayer player;
    private final CellLocation from;
    private CellLocation to;

    public PlayerMoveEvent(
            CellPlayer player,
            CellLocation from,
            CellLocation to
    ) {
        this.player = Objects.requireNonNull(player, "player");
        this.from = Objects.requireNonNull(from, "from");
        this.to = Objects.requireNonNull(to, "to");
    }

    public CellPlayer player() {
        return player;
    }

    public CellLocation from() {
        return from;
    }

    public CellLocation to() {
        return to;
    }

    public void to(CellLocation to) {
        this.to = Objects.requireNonNull(to, "to");
    }

    public boolean changedBlock() {
        return !from.world.equals(to.world)
                || (int) Math.floor(from.x) != (int) Math.floor(to.x)
                || (int) Math.floor(from.y) != (int) Math.floor(to.y)
                || (int) Math.floor(from.z) != (int) Math.floor(to.z);
    }

}
