package top.likoslupus.cellulosesz.api.teleport;

import top.likoslupus.cellulosesz.api.text.LocalizedMessage;

import java.util.Map;
import java.util.Optional;

import static java.util.Objects.requireNonNull;

public record TeleportResult(
        TeleportStatus status,
        Optional<CellLocation> destination,
        LocalizedMessage message
) {

    public TeleportResult {
        requireNonNull(status, "status");
        destination = requireNonNull(destination, "destination").map(TeleportResult::copy);
        requireNonNull(message, "message");
        if (status == TeleportStatus.SUCCESS && destination.isEmpty()) {
            throw new IllegalArgumentException("Successful teleport must expose its destination");
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

    public static TeleportResult success(CellLocation location) {
        return new TeleportResult(
                TeleportStatus.SUCCESS,
                Optional.of(copy(location)),
                LocalizedMessage.of("service.teleport.success")
        );
    }

    public static TeleportResult failed(TeleportStatus status, String key) {
        if (status == TeleportStatus.SUCCESS) {
            throw new IllegalArgumentException("failure status required");
        }
        return new TeleportResult(
                status,
                Optional.empty(),
                LocalizedMessage.of(key)
        );
    }

    public static TeleportResult failed(
            TeleportStatus status,
            String key,
            Map<String, ?> placeholders
    ) {
        if (status == TeleportStatus.SUCCESS) {
            throw new IllegalArgumentException("failure status required");
        }
        return new TeleportResult(
                status,
                Optional.empty(),
                LocalizedMessage.of(key, placeholders)
        );
    }

    @Override
    public Optional<CellLocation> destination() {
        return destination.map(TeleportResult::copy);
    }

    public boolean success() {
        return status == TeleportStatus.SUCCESS;
    }

}
