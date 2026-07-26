package top.likoslupus.cellulosesz.api.teleport;

import java.util.UUID;

import static java.util.Objects.requireNonNull;

public record TeleportRequest(
        UUID id,
        UUID requester,
        UUID target,
        TeleportRequestType type,
        long createdAtMillis,
        long expiresAtMillis
) {

    public TeleportRequest {
        requireNonNull(id, "id");
        requireNonNull(requester, "requester");
        requireNonNull(target, "target");
        requireNonNull(type, "type");
        if (requester.equals(target)) {
            throw new IllegalArgumentException("A teleport request cannot target its requester");
        }
        if (createdAtMillis < 0L || expiresAtMillis <= createdAtMillis) {
            throw new IllegalArgumentException("Invalid teleport request lifetime");
        }
    }

    public boolean expired(long now) {
        return expiresAtMillis <= now;
    }

}
