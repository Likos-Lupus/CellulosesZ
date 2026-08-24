package top.likoslupus.cellulosesz.modules.admin.domain;

import top.likoslupus.cellulosesz.api.teleport.CellLocation;
import top.likoslupus.cellulosesz.common.admin.Expiration;

import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

import static top.likoslupus.cellulosesz.api.validation.Checks.requireNonBlank;

import static java.util.Objects.requireNonNull;

public record JailedPlayer(
        UUID uuid,
        String name,
        String jail,
        String reason,
        String actor,
        Instant createdAt,
        Expiration expiration,
        Optional<CellLocation> returnLocation,
        JailState state
) {

    public JailedPlayer {
        requireNonNull(uuid, "uuid");
        name = requireNonBlank(name, "name").trim();
        jail = requireNonBlank(jail, "jail").trim();
        reason = requireNonNull(reason, "reason");
        actor = requireNonBlank(actor, "actor").trim();
        requireNonNull(createdAt, "createdAt");
        requireNonNull(expiration, "expiration");
        requireNonNull(returnLocation, "returnLocation");
        requireNonNull(state, "state");
    }

    public boolean expired(Instant now) {
        return state == JailState.ACTIVE && expiration.expired(now);
    }

}
