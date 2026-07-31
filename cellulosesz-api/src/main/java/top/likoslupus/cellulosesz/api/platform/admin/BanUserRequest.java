package top.likoslupus.cellulosesz.api.platform.admin;

import top.likoslupus.cellulosesz.api.admin.Expiration;

import java.time.Instant;

import static java.util.Objects.requireNonNull;
import static top.likoslupus.cellulosesz.api.validation.Checks.requireMaxLength;
import static top.likoslupus.cellulosesz.api.validation.Checks.requireNoControlCharacters;

public record BanUserRequest(
        PlayerProfileId target,
        BanActor actor,
        String reason,
        Instant createdAt,
        Expiration expiration
) {

    public BanUserRequest {
        requireNonNull(target, "target");
        requireNonNull(actor, "actor");
        reason = requireNonNull(reason, "reason");
        requireMaxLength(reason, 512, "reason");
        requireNoControlCharacters(reason, "reason");
        requireNonNull(createdAt, "createdAt");
        requireNonNull(expiration, "expiration");
        expiration.expiresAt().ifPresent(expiresAt -> {
            if (!expiresAt.isAfter(createdAt)) {
                throw new IllegalArgumentException("expiration must be after createdAt");
            }
        });
    }

}
