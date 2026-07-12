package top.likoslupus.cellulosesz.api.event;

import top.likoslupus.cellulosesz.api.platform.CellPlayer;

import java.util.Objects;

public final class PlayerPickupEvent extends AbstractCancellableEvent {

    private final CellPlayer player;
    private final String itemId;
    private final int count;

    public PlayerPickupEvent(
            CellPlayer player,
            String itemId,
            int count
    ) {
        this.player = Objects.requireNonNull(player, "player");
        this.itemId = Objects.requireNonNull(itemId, "itemId");
        this.count = count;
    }

    public CellPlayer player() {
        return player;
    }

    public String itemId() {
        return itemId;
    }

    public int count() {
        return count;
    }

}
