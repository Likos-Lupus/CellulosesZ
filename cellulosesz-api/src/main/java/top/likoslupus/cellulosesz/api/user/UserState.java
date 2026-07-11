package top.likoslupus.cellulosesz.api.user;

import org.jspecify.annotations.Nullable;

import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Set;

public final class UserState {

    public boolean afk;
    public boolean god;
    public boolean flying;
    public boolean vanished;
    public @Nullable Long mutedUntil;
    public @Nullable String nickname;
    public Map<String, String> powerTools = new LinkedHashMap<>();
    public Set<String> unlimitedItems = new LinkedHashSet<>();

}
