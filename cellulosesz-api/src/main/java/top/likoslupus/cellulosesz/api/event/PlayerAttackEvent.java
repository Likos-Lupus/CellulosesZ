package top.likoslupus.cellulosesz.api.event;

import top.likoslupus.cellulosesz.api.platform.CellPlayer;

import java.util.Objects;
import java.util.Optional;
import java.util.UUID;

public final class PlayerAttackEvent extends AbstractCancellableEvent {

    private final CellPlayer player;
    private final Optional<UUID> targetPlayer;
    private final String targetType;

    public PlayerAttackEvent(
            CellPlayer player,
            Optional<UUID> targetPlayer,
            String targetType
    ) {
        this.player = Objects.requireNonNull(player, "player");
        this.targetPlayer = Objects.requireNonNull(targetPlayer, "targetPlayer");
        this.targetType = Objects.requireNonNull(targetType, "targetType");
    }

    public CellPlayer player() {
        return player;
    }

    public Optional<UUID> targetPlayer() {
        return targetPlayer;
    }

    public String targetType() {
        return targetType;
    }

}
