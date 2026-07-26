package top.likoslupus.cellulosesz.api.user;

import org.jspecify.annotations.Nullable;

public final class UserTimestamps {

    public long firstJoin;
    public long lastJoin;
    public long lastQuit;
    public long playTimeMillis;
    public long lastActivityAt;
    public @Nullable Long activeSessionStartedAt;

}
