package top.likoslupus.cellulosesz.common.world;

import static top.likoslupus.cellulosesz.api.validation.Checks.requirePositive;

import static java.util.Objects.requireNonNull;

public record SpawnerRequest(
        String entityId,
        int delayTicks
) {

    public SpawnerRequest {
        entityId = requireNonNull(entityId, "entityId");
        requirePositive(delayTicks, "delayTicks");
    }

}
