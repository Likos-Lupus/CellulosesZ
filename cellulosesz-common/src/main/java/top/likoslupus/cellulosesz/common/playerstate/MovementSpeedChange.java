package top.likoslupus.cellulosesz.common.playerstate;

import top.likoslupus.cellulosesz.common.platform.MovementSpeedType;

import static top.likoslupus.cellulosesz.api.validation.Checks.requireFinite;

import static java.util.Objects.requireNonNull;

public record MovementSpeedChange(
        MovementSpeedType type,
        double previous,
        double current
) {

    public MovementSpeedChange {
        requireNonNull(type, "type");
        requireFinite(previous, "previous");
        requireFinite(current, "current");
    }

}
