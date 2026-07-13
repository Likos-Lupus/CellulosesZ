package top.likoslupus.cellulosesz.api.admin;

import org.jspecify.annotations.Nullable;

import top.likoslupus.cellulosesz.api.teleport.CellLocation;

import java.util.UUID;

public final class JailedPlayer {

    public UUID uuid = new UUID(0L, 0L);
    public String name = "";
    public String jail = "";
    public String reason = "";
    public String actor = "console";
    public long createdAt;
    public @Nullable Long expiresAt;
    public @Nullable CellLocation returnLocation;

    public boolean expired(long now) {
        return expiresAt != null && expiresAt <= now;
    }

}
