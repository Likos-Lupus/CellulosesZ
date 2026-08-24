package top.likoslupus.cellulosesz.common.world;

import java.util.OptionalInt;

import static top.likoslupus.cellulosesz.api.validation.Checks.requireNonNegative;

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
        requireNonNegative(loadedChunks, "loadedChunks");
        requireNonNegative(entities, "entities");
        blockEntities.stream().forEach(count ->
                requireNonNegative(count, "blockEntities count")
        );
    }

}
