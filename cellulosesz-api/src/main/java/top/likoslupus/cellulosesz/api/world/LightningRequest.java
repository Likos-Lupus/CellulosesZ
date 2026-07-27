package top.likoslupus.cellulosesz.api.world;

import top.likoslupus.cellulosesz.api.teleport.CellLocation;

import static java.util.Objects.requireNonNull;

public record LightningRequest(
        CellLocation location,
        boolean visualOnly,
        double additionalDamage
) {

    public LightningRequest {
        requireNonNull(location, "location");
        if (!Double.isFinite(additionalDamage) || additionalDamage < 0.0D) {
            throw new IllegalArgumentException("additionalDamage must be finite and non-negative");
        }
    }

}
