package top.likoslupus.cellulosesz.api.admin;

import org.jspecify.annotations.Nullable;

import java.util.UUID;

public final class BanRecord {

    public int schema = 1;
    public @Nullable UUID uuid;
    public String name = "";
    public String reason = "";
    public String actor = "console";
    public long createdAt;
    public @Nullable Long expiresAt;
    public boolean ip;
    public @Nullable String address;

    public boolean expired(long now) {
        return expiresAt != null && expiresAt <= now;
    }

}
