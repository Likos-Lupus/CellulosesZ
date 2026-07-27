package top.likoslupus.cellulosesz.api.entity;

import top.likoslupus.cellulosesz.api.platform.CellPlayer;

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
        if (!Double.isFinite(speed) || speed <= 0.0D) {
            throw new IllegalArgumentException("speed must be finite and positive");
        }
        if (lifetimeTicks < 1) {
            throw new IllegalArgumentException("lifetimeTicks must be positive");
        }
        if (!Double.isFinite(explosionPower) || explosionPower < 0.0D) {
            throw new IllegalArgumentException("explosionPower must be finite and non-negative");
        }
    }

}
