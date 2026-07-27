package top.likoslupus.cellulosesz.api.entity;

import java.util.UUID;

import static java.util.Objects.requireNonNull;

public record TemporaryMobResult(
        UUID entityUuid,
        TemporaryMobType type
) {

    public TemporaryMobResult {
        requireNonNull(entityUuid, "entityUuid");
        requireNonNull(type, "type");
    }

}
