package top.likoslupus.cellulosesz.api.user;

import org.jspecify.annotations.Nullable;

public final class UserState {

    public boolean afk;
    public boolean god;
    public boolean flying;
    public boolean vanished;
    public @Nullable Long mutedUntil;
    public @Nullable String nickname;

}
