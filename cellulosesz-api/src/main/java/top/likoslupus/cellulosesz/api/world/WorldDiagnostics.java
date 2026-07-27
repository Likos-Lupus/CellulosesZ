package top.likoslupus.cellulosesz.api.world;

import java.util.OptionalInt;

import static java.util.Objects.requireNonNull;

public record WorldDiagnostics(
        String worldId,
        int loadedChunks,
        int entities,
        OptionalInt blockEntities
) {

    public WorldDiagnostics {
        worldId = requireNonNull(worldId, "worldId");
        requireNonNull(blockEntities, "blockEntities");
        if (loadedChunks < 0
                || entities < 0
                || blockEntities.stream().anyMatch(count -> count < 0)
        ) {
            throw new IllegalArgumentException("diagnostic counts must not be negative");
        }
    }

}
