package top.likoslupus.cellulosesz.api.entity;

import top.likoslupus.cellulosesz.api.teleport.CellLocation;

import static java.util.Objects.requireNonNull;
import static top.likoslupus.cellulosesz.api.validation.Checks.*;

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
        requirePositive(amount, "amount");
        requirePositive(fuseTicks, "fuseTicks");
        requireNonNegative(explosionPower, "explosionPower");
        requireNonNegative(spread, "spread");
        requireFinite(height, "height");
    }

}
