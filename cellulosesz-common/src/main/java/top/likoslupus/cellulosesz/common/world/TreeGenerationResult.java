package top.likoslupus.cellulosesz.common.world;

import top.likoslupus.cellulosesz.api.teleport.CellLocation;

import static java.util.Objects.requireNonNull;

public record TreeGenerationResult(
        TreeType type,
        CellLocation location
) {

    public TreeGenerationResult {
        requireNonNull(type, "type");
        requireNonNull(location, "location");
    }

}
