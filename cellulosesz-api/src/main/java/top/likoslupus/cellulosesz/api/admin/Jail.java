package top.likoslupus.cellulosesz.api.admin;

import top.likoslupus.cellulosesz.api.teleport.CellLocation;

import java.time.Instant;

import static top.likoslupus.cellulosesz.api.validation.TextChecks.requireNonBlank;

import static java.util.Objects.requireNonNull;

public record Jail(
        String name,
        CellLocation location,
        String createdBy,
        Instant createdAt
) {

    public Jail {
        name = requireNonBlank(name, "name").trim();
        requireNonNull(location, "location");
        createdBy = requireNonBlank(createdBy, "createdBy").trim();
        requireNonNull(createdAt, "createdAt");
    }

}
