package top.likoslupus.cellulosesz.modules.admin.domain;

import top.likoslupus.cellulosesz.common.admin.Expiration;

import java.net.InetAddress;
import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

import static top.likoslupus.cellulosesz.api.validation.Checks.requireNonBlank;

import static java.util.Objects.requireNonNull;

/**
 * Immutable player/IP punishment snapshot. Storage DTOs are module-internal.
 */
public record BanRecord(
        Optional<UUID> uuid,
        String name,
        String reason,
        AdminActor actor,
        Instant createdAt,
        Expiration expiration,
        boolean ip,
        Optional<InetAddress> address
) {

    public BanRecord {
        requireNonNull(uuid, "uuid");
        name = requireNonBlank(name, "name").trim();
        reason = requireNonNull(reason, "reason");
        requireNonNull(actor, "actor");
        requireNonNull(createdAt, "createdAt");
        requireNonNull(expiration, "expiration");
        requireNonNull(address, "address");
        if (ip && (address.isEmpty() || uuid.isPresent())) {
            throw new IllegalStateException("IP records require an address and no UUID");
        }
        if (!ip && (address.isPresent() || uuid.isEmpty())) {
            throw new IllegalStateException("Player records require a UUID and no address");
        }
        expiration.expiresAt().ifPresent(value -> {
            if (!value.isAfter(createdAt)) {
                throw new IllegalArgumentException("expiration must follow creation");
            }
        });
    }

    public static BanRecord player(
            UUID uuid,
            String name,
            String reason,
            AdminActor actor,
            Instant createdAt,
            Expiration expiration
    ) {
        return new BanRecord(
                Optional.of(uuid),
                name,
                reason,
                actor,
                createdAt,
                expiration,
                false,
                Optional.empty()
        );
    }

    public static BanRecord address(
            InetAddress address,
            String reason,
            AdminActor actor,
            Instant createdAt,
            Expiration expiration
    ) {
        var canonical = address.getHostAddress();
        return new BanRecord(
                Optional.empty(),
                canonical,
                reason,
                actor,
                createdAt,
                expiration,
                true,
                Optional.of(address)
        );
    }

    public boolean expired(Instant now) {
        return expiration.expired(now);
    }

}
