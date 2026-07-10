package top.likoslupus.cellulosesz.api.teleport;

import java.util.UUID;

public record TeleportRequest(
        UUID requester,
        UUID target,
        TeleportRequestType type,
        long createdAtMillis,
        long expiresAtMillis
) {

    public boolean expired(long now) {
        return expiresAtMillis <= now;
    }

}
