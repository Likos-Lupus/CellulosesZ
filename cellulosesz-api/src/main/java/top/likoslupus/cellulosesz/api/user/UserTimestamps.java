package top.likoslupus.cellulosesz.api.user;

import org.jspecify.annotations.Nullable;

import static top.likoslupus.cellulosesz.api.validation.Checks.requireNonNegative;

public record UserTimestamps(
        long firstJoin,
        long lastJoin,
        long lastQuit,
        long playTimeMillis,
        long lastActivityAt,
        @Nullable Long activeSessionStartedAt
) {

    public UserTimestamps {
        requireNonNegative(firstJoin, "firstJoin");
        requireNonNegative(lastJoin, "lastJoin");
        requireNonNegative(lastQuit, "lastQuit");
        requireNonNegative(playTimeMillis, "playTimeMillis");
        requireNonNegative(lastActivityAt, "lastActivityAt");
        if (activeSessionStartedAt != null) {
            requireNonNegative(activeSessionStartedAt, "activeSessionStartedAt");
        }
    }

    public static UserTimestamps defaults() {
        return new UserTimestamps(
                0,
                0,
                0,
                0,
                0,
                null
        );
    }

    public UserTimestamps withFirstJoin(long value) {
        return new UserTimestamps(value, lastJoin, lastQuit, playTimeMillis, lastActivityAt, activeSessionStartedAt);
    }

    public UserTimestamps withLastJoin(long value) {
        return new UserTimestamps(firstJoin, value, lastQuit, playTimeMillis, lastActivityAt, activeSessionStartedAt);
    }

    public UserTimestamps withLastQuit(long value) {
        return new UserTimestamps(firstJoin, lastJoin, value, playTimeMillis, lastActivityAt, activeSessionStartedAt);
    }

    public UserTimestamps withPlayTimeMillis(long value) {
        return new UserTimestamps(firstJoin, lastJoin, lastQuit, value, lastActivityAt, activeSessionStartedAt);
    }

    public UserTimestamps withLastActivityAt(long value) {
        return new UserTimestamps(firstJoin, lastJoin, lastQuit, playTimeMillis, value, activeSessionStartedAt);
    }

    public UserTimestamps withActiveSessionStartedAt(@Nullable Long value) {
        return new UserTimestamps(firstJoin, lastJoin, lastQuit, playTimeMillis, lastActivityAt, value);
    }

}
