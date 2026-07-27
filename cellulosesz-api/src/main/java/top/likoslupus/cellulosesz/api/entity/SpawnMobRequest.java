package top.likoslupus.cellulosesz.api.entity;

import top.likoslupus.cellulosesz.api.platform.CellPlayer;

import static java.util.Objects.requireNonNull;

public record SpawnMobRequest(
        String entityId,
        int amount,
        CellPlayer anchor
) {

    public SpawnMobRequest {
        entityId = requireNonNull(entityId, "entityId");
        if (amount < 1) {
            throw new IllegalArgumentException("amount must be positive");
        }
        requireNonNull(anchor, "anchor");
    }

}
