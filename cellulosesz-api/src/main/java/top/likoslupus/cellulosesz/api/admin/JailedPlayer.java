package top.likoslupus.cellulosesz.api.admin;

import org.jspecify.annotations.Nullable;

import java.util.UUID;

public final class JailedPlayer {

    public int schema = 1;
    public UUID uuid = new UUID(0L, 0L);
    public String name = "";
    public String jail = "";
    public String reason = "";
    public String actor = "console";
    public long createdAt;
    public @Nullable Long expiresAt;

    public boolean expired(long now) {
        return expiresAt != null && expiresAt <= now;
    }

}
