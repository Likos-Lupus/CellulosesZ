package top.likoslupus.cellulosesz.api.world;

import static java.util.Objects.requireNonNull;

public record SpawnerResult(
        String entityId,
        int delayTicks
) {

    public SpawnerResult {
        entityId = requireNonNull(entityId, "entityId");
        if (delayTicks < 1) {
            throw new IllegalArgumentException("delayTicks must be positive");
        }
    }

}
