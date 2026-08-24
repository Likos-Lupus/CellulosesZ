package top.likoslupus.cellulosesz.common.entity;

import static java.util.Objects.requireNonNull;

public record SpawnMobResult(
        String entityId,
        int requested,
        int spawned
) {

    public SpawnMobResult {
        entityId = requireNonNull(entityId, "entityId");
        if (requested < 1 || spawned < 0 || spawned > requested) {
            throw new IllegalArgumentException("invalid spawn counts");
        }
    }

}
