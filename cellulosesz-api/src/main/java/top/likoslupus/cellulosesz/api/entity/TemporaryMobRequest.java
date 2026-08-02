package top.likoslupus.cellulosesz.api.entity;

import top.likoslupus.cellulosesz.api.platform.CellPlayer;

import static top.likoslupus.cellulosesz.api.validation.NumericChecks.requireNonNegative;
import static top.likoslupus.cellulosesz.api.validation.NumericChecks.requirePositive;

import static java.util.Objects.requireNonNull;

public record TemporaryMobRequest(
        CellPlayer shooter,
        TemporaryMobType type,
        double speed,
        int lifetimeTicks,
        double explosionPower,
        boolean blockDamage
) {

    public TemporaryMobRequest {
        requireNonNull(shooter, "shooter");
        requireNonNull(type, "type");
        requirePositive(speed, "speed");
        requirePositive(lifetimeTicks, "lifetimeTicks");
        requireNonNegative(explosionPower, "explosionPower");
    }

}
