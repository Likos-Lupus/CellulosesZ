package top.likoslupus.cellulosesz.common.entity;

import top.likoslupus.cellulosesz.api.platform.CellPlayer;

import static top.likoslupus.cellulosesz.api.validation.Checks.requirePositive;

import static java.util.Objects.requireNonNull;

public record ProjectileRequest(
        CellPlayer shooter,
        ProjectileType type,
        double speed,
        int lifetimeTicks
) {

    public ProjectileRequest {
        requireNonNull(shooter, "shooter");
        requireNonNull(type, "type");
        requirePositive(speed, "speed");
        requirePositive(lifetimeTicks, "lifetimeTicks");
    }

}
