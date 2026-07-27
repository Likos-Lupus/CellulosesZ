package top.likoslupus.cellulosesz.api.world;

import static java.util.Objects.requireNonNull;

public record SpawnerRequest(
        String entityId,
        int delayTicks
) {

    public SpawnerRequest {
        entityId = requireNonNull(entityId, "entityId");
        if (delayTicks < 1) {
            throw new IllegalArgumentException("delayTicks must be positive");
        }
    }

}
