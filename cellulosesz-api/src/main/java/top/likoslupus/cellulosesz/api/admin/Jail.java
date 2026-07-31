package top.likoslupus.cellulosesz.api.admin;

import top.likoslupus.cellulosesz.api.teleport.CellLocation;

import java.time.Instant;

import static java.util.Objects.requireNonNull;
import static top.likoslupus.cellulosesz.api.validation.Checks.requireNonBlank;

public record Jail(
        String name,
        CellLocation location,
        String createdBy,
        Instant createdAt
) {

    public Jail {
        name = requireNonBlank(name, "name").trim();
        location = copy(requireNonNull(location, "location"));
        createdBy = requireNonBlank(createdBy, "createdBy").trim();
        requireNonNull(createdAt, "createdAt");
    }

    private static CellLocation copy(CellLocation value) {
        return new CellLocation(
                value.world,
                value.x, value.y, value.z,
                value.yaw, value.pitch
        );
    }

    @Override
    public CellLocation location() {
        return copy(location);
    }

}
