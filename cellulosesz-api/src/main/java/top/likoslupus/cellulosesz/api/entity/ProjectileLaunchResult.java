package top.likoslupus.cellulosesz.api.entity;

import java.util.UUID;

import static java.util.Objects.requireNonNull;

public record ProjectileLaunchResult(
        UUID entityUuid,
        ProjectileType type
) {

    public ProjectileLaunchResult {
        requireNonNull(entityUuid, "entityUuid");
        requireNonNull(type, "type");
    }

}
