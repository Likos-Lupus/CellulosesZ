package top.likoslupus.cellulosesz.api.admin;

import org.jspecify.annotations.Nullable;

import java.util.UUID;

import static java.util.Objects.requireNonNull;

public record BanRecord(
        @Nullable UUID uuid,
        String name,
        String reason,
        String actor,
        long createdAt,
        @Nullable Long expiresAt,
        boolean ip,
        @Nullable String address
) {

    public BanRecord {
        name = requireNonNull(name, "name").trim();
        reason = requireNonNull(reason, "reason");
        actor = requireNonNull(actor, "actor").trim();

        if (name.isEmpty()) {
            throw new IllegalArgumentException("name must not be blank");
        }
        if (actor.isEmpty()) {
            throw new IllegalArgumentException("actor must not be blank");
        }
        if (createdAt < 0L) {
            throw new IllegalArgumentException("createdAt must not be negative");
        }
        if (expiresAt != null && expiresAt <= createdAt) {
            throw new IllegalArgumentException("expiresAt must be after createdAt");
        }

        if (ip) {
            address = requireNonNull(address, "address").trim();
            if (address.isEmpty()) {
                throw new IllegalArgumentException("IP record address must not be blank");
            }
            if (uuid != null) {
                throw new IllegalArgumentException("IP records must not contain a UUID");
            }
        } else if (address != null) {
            throw new IllegalArgumentException("Player records must not contain an IP address");
        }
    }

    public boolean expired(long now) {
        return expiresAt != null && expiresAt <= now;
    }

}
