package top.likoslupus.cellulosesz.api.user;

import org.jspecify.annotations.Nullable;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.UUID;

public final class CellUser {

    public UUID uuid;
    public @Nullable String lastKnownName;
    public UserTimestamps timestamps = new UserTimestamps();
    public UserState state = new UserState();
    public UserPreferences preferences = new UserPreferences();
    public UserRelations relations = new UserRelations();
    public Map<String, Long> cooldowns = new LinkedHashMap<>();

    public CellUser() {
        this.uuid = new UUID(0L, 0L);
    }

    public CellUser(UUID uuid) {
        this.uuid = uuid;
    }

}
