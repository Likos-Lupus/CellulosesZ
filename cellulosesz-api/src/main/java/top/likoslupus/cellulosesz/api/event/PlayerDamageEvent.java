package top.likoslupus.cellulosesz.api.event;

import top.likoslupus.cellulosesz.api.platform.CellPlayer;

import java.util.Objects;
import java.util.Optional;
import java.util.UUID;

public final class PlayerDamageEvent extends AbstractCancellableEvent {

    private final CellPlayer player;
    private final String source;
    private final Optional<UUID> attacker;
    private final float amount;

    public PlayerDamageEvent(
            CellPlayer player,
            String source,
            Optional<UUID> attacker,
            float amount
    ) {
        this.player = Objects.requireNonNull(player, "player");
        this.source = Objects.requireNonNull(source, "source");
        this.attacker = Objects.requireNonNull(attacker, "attacker");
        this.amount = amount;
    }

    public CellPlayer player() {
        return player;
    }

    public String source() {
        return source;
    }

    public Optional<UUID> attacker() {
        return attacker;
    }

    public float amount() {
        return amount;
    }

}
