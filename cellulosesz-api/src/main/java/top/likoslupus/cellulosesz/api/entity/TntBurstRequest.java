package top.likoslupus.cellulosesz.api.entity;

import top.likoslupus.cellulosesz.api.teleport.CellLocation;

import static java.util.Objects.requireNonNull;

public record TntBurstRequest(
        CellLocation center,
        int amount,
        int fuseTicks,
        double explosionPower,
        boolean blockDamage,
        double spread,
        double height
) {

    public TntBurstRequest {
        requireNonNull(center, "center");
        if (amount < 1 || fuseTicks < 1) {
            throw new IllegalArgumentException("amount and fuseTicks must be positive");
        }
        if (!Double.isFinite(explosionPower) || explosionPower < 0.0D) {
            throw new IllegalArgumentException("explosionPower must be finite and non-negative");
        }
        if (!Double.isFinite(spread) || spread < 0.0D || !Double.isFinite(height)) {
            throw new IllegalArgumentException("spread and height must be finite");
        }
    }

}
