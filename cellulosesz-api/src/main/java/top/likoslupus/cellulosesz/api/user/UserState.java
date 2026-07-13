package top.likoslupus.cellulosesz.api.user;

import org.jspecify.annotations.Nullable;

import java.util.*;

public final class UserState {

    public boolean afk;
    public boolean god;
    public boolean flying;
    public boolean vanished;
    public @Nullable String nickname;
    public Map<String, List<String>> powerToolCommands = new LinkedHashMap<>();
    public Set<String> unlimitedItems = new LinkedHashSet<>();

}
