package top.likoslupus.cellulosesz.api.entity;

import top.likoslupus.cellulosesz.api.platform.CellPlayer;

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
        if (!Double.isFinite(speed) || speed <= 0.0D) {
            throw new IllegalArgumentException("speed must be finite and positive");
        }
        if (lifetimeTicks < 1) {
            throw new IllegalArgumentException("lifetimeTicks must be positive");
        }
    }

}
