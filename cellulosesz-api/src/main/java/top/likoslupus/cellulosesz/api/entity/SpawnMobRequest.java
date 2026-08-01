package top.likoslupus.cellulosesz.api.entity;

import top.likoslupus.cellulosesz.api.platform.CellPlayer;

import static java.util.Objects.requireNonNull;
import static top.likoslupus.cellulosesz.api.validation.Checks.requirePositive;

public record SpawnMobRequest(
        String entityId,
        int amount,
        CellPlayer anchor
) {

    public SpawnMobRequest {
        entityId = requireNonNull(entityId, "entityId");
        requirePositive(amount, "amount");
        requireNonNull(anchor, "anchor");
    }

}
