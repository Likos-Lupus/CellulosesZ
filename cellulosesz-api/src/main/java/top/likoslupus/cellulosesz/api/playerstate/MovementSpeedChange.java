package top.likoslupus.cellulosesz.api.playerstate;

import top.likoslupus.cellulosesz.api.platform.MovementSpeedType;

import static java.util.Objects.requireNonNull;

public record MovementSpeedChange(
        MovementSpeedType type,
        double previous,
        double current
) {

    public MovementSpeedChange {
        requireNonNull(type, "type");
        if (!Double.isFinite(previous) || !Double.isFinite(current)) {
            throw new IllegalArgumentException("movement speed must be finite");
        }
    }

}
