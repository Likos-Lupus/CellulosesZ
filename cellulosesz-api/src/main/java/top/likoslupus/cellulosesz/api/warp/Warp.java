package top.likoslupus.cellulosesz.api.warp;

import top.likoslupus.cellulosesz.api.teleport.CellLocation;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

import static top.likoslupus.cellulosesz.api.validation.NumericChecks.requireNonNegative;
import static top.likoslupus.cellulosesz.api.validation.TextChecks.requireNonBlank;

import static java.util.Objects.requireNonNull;

/** Immutable warp value. */
public record Warp(
        String name,
        String displayName,
        BigDecimal cost,
        CellLocation location,
        Optional<UUID> createdBy,
        Instant createdAt
) {

    public Warp(String name, CellLocation location) {
        this(
                name,
                name,
                BigDecimal.ZERO,
                location,
                Optional.empty(),
                Instant.now()
        );
    }

    public Warp {
        name = requireNonBlank(requireNonNull(name, "name").trim(), "name");
        displayName = requireNonBlank(
                requireNonNull(displayName, "displayName").trim(),
                "displayName"
        );
        cost = requireNonNegative(requireNonNull(cost, "cost"), "cost");
        requireNonNull(location, "location");
        requireNonNull(createdBy, "createdBy");
        requireNonNull(createdAt, "createdAt");
    }

}
