package top.likoslupus.cellulosesz.api.teleport;

import top.likoslupus.cellulosesz.api.text.LocalizedMessage;
import top.likoslupus.cellulosesz.api.text.MessageArguments;

import java.util.Optional;

import static java.util.Objects.requireNonNull;

public record TeleportResult(
        TeleportStatus status,
        Optional<CellLocation> destination,
        LocalizedMessage message
) {

    public TeleportResult {
        requireNonNull(status, "status");
        requireNonNull(destination, "destination");
        requireNonNull(message, "message");
        if (status == TeleportStatus.SUCCESS && destination.isEmpty()) {
            throw new IllegalArgumentException("Successful teleport must expose its destination");
        }
    }

    public static TeleportResult success(CellLocation location) {
        return new TeleportResult(
                TeleportStatus.SUCCESS,
                Optional.of(requireNonNull(location, "location")),
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
            MessageArguments arguments
    ) {
        if (status == TeleportStatus.SUCCESS) {
            throw new IllegalArgumentException("failure status required");
        }
        return new TeleportResult(
                status,
                Optional.empty(),
                LocalizedMessage.of(key, arguments)
        );
    }

    public boolean success() {
        return status == TeleportStatus.SUCCESS;
    }

}
