package top.likoslupus.cellulosesz.common.world;

import static top.likoslupus.cellulosesz.api.validation.Checks.requirePositive;

import static java.util.Objects.requireNonNull;

public record SpawnerResult(
        String entityId,
        int delayTicks
) {

    public SpawnerResult {
        entityId = requireNonNull(entityId, "entityId");
        requirePositive(delayTicks, "delayTicks");
    }

}
