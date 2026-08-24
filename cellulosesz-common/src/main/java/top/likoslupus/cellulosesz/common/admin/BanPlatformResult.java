package top.likoslupus.cellulosesz.common.admin;

import org.jspecify.annotations.Nullable;

import static top.likoslupus.cellulosesz.api.validation.Checks.requireNonNegative;

import static java.util.Objects.requireNonNull;

public record BanPlatformResult(
        BanPlatformStatus status,
        int disconnectedPlayers,
        @Nullable String detail
) {

    public BanPlatformResult {
        requireNonNull(status, "status");
        requireNonNegative(disconnectedPlayers, "disconnectedPlayers");
        if (!status.successful() && disconnectedPlayers != 0) {
            throw new IllegalArgumentException("Failure results cannot report disconnected players");
        }
    }

    public static BanPlatformResult success() {
        return new BanPlatformResult(
                BanPlatformStatus.SUCCESS,
                0,
                null
        );
    }

    public static BanPlatformResult success(int disconnectedPlayers) {
        return new BanPlatformResult(
                BanPlatformStatus.SUCCESS,
                disconnectedPlayers,
                null
        );
    }

    public static BanPlatformResult failure(BanPlatformStatus status) {
        if (status.successful()) {
            throw new IllegalArgumentException("Failure status must not be successful");
        }
        return new BanPlatformResult(status, 0, null);
    }

    public static BanPlatformResult failure(BanPlatformStatus status, String detail) {
        if (status.successful()) {
            throw new IllegalArgumentException("Failure status must not be successful");
        }
        return new BanPlatformResult(
                status,
                0,
                requireNonNull(detail, "detail")
        );
    }

}
