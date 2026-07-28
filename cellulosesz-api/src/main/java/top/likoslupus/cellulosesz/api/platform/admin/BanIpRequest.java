package top.likoslupus.cellulosesz.api.platform.admin;

import org.jspecify.annotations.Nullable;

import java.net.InetAddress;
import java.time.Instant;

import static java.util.Objects.requireNonNull;
import static top.likoslupus.cellulosesz.api.validation.Checks.requireMaxLength;
import static top.likoslupus.cellulosesz.api.validation.Checks.requireNoControlCharacters;

public record BanIpRequest(
        InetAddress target,
        BanActor actor,
        String reason,
        Instant createdAt,
        @Nullable Instant expiresAt
) {

    public BanIpRequest {
        requireNonNull(target, "target");
        requireNonNull(actor, "actor");
        reason = requireNonNull(reason, "reason");
        requireMaxLength(reason, 512, "reason");
        requireNoControlCharacters(reason, "reason");
        requireNonNull(createdAt, "createdAt");
        if (expiresAt != null && !expiresAt.isAfter(createdAt)) {
            throw new IllegalArgumentException("expiresAt must be after createdAt");
        }
    }

}
