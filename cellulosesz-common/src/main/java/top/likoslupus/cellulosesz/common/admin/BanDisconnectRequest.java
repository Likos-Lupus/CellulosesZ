package top.likoslupus.cellulosesz.common.admin;

import java.net.InetAddress;
import java.util.UUID;
import org.jspecify.annotations.Nullable;

import static top.likoslupus.cellulosesz.api.validation.Checks.requireMaxLength;
import static top.likoslupus.cellulosesz.api.validation.Checks.requireNoControlCharacters;

import static java.util.Objects.requireNonNull;

public record BanDisconnectRequest(
        @Nullable UUID userId,
        @Nullable InetAddress address,
        String reason
) {

    public BanDisconnectRequest {
        if ((userId == null) == (address == null)) {
            throw new IllegalArgumentException("Exactly one of userId or address must be supplied");
        }
        reason = requireNonNull(reason, "reason");
        requireMaxLength(reason, 512, "reason");
        requireNoControlCharacters(reason, "reason");
    }

    public static BanDisconnectRequest user(UUID userId, String reason) {
        return new BanDisconnectRequest(
                requireNonNull(userId, "userId"),
                null,
                reason
        );
    }

    public static BanDisconnectRequest address(InetAddress address, String reason) {
        return new BanDisconnectRequest(
                null,
                requireNonNull(address, "address"),
                reason
        );
    }

}
