package top.likoslupus.cellulosesz.common.world;

import top.likoslupus.cellulosesz.api.platform.CellPlayer;

import java.util.Optional;

import static top.likoslupus.cellulosesz.api.validation.Checks.requirePositive;

import static java.util.Objects.requireNonNull;

public record EntityRemovalRequest(
        EntityRemoveSelector selector,
        Optional<CellPlayer> origin,
        int radius
) {

    public EntityRemovalRequest {
        requireNonNull(selector, "selector");
        requireNonNull(origin, "origin");
        requirePositive(radius, "radius");
    }

}
