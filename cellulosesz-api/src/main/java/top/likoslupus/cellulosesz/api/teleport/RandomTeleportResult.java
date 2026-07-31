package top.likoslupus.cellulosesz.api.teleport;

import java.util.Optional;

import static java.util.Objects.requireNonNull;

public record RandomTeleportResult(
        RandomTeleportStatus status,
        Optional<CellLocation> location
) {

    public RandomTeleportResult {
        requireNonNull(status, "status");
        location = requireNonNull(location, "location").map(RandomTeleportResult::copy);
        if (status == RandomTeleportStatus.SUCCESS && location.isEmpty()) {
            throw new IllegalArgumentException("successful result requires a location");
        }
    }

    private static CellLocation copy(CellLocation value) {
        requireNonNull(value, "location");
        return new CellLocation(
                value.world,
                value.x, value.y, value.z,
                value.yaw, value.pitch
        );
    }

    public static RandomTeleportResult success(CellLocation location) {
        return new RandomTeleportResult(RandomTeleportStatus.SUCCESS, Optional.of(copy(location)));
    }

    public static RandomTeleportResult failure(RandomTeleportStatus status) {
        if (status == RandomTeleportStatus.SUCCESS) {
            throw new IllegalArgumentException("failure status required");
        }
        return new RandomTeleportResult(status, Optional.empty());
    }

    public boolean success() {
        return status == RandomTeleportStatus.SUCCESS;
    }

}
