package top.likoslupus.cellulosesz.modules.user.persistence;

import java.util.*;
import org.jspecify.annotations.Nullable;

/** Mutable Jackson representation of a persisted user. */
public final class UserDocument {

    public @Nullable String uuid;
    public @Nullable String lastKnownName;
    public UserTimestampsDocument timestamps = new UserTimestampsDocument();
    public UserStateDocument state = new UserStateDocument();
    public UserPreferencesDocument preferences = new UserPreferencesDocument();
    public UserRelationsDocument relations = new UserRelationsDocument();
    public Map<String, Long> cooldowns = new LinkedHashMap<>();

    public static final class UserTimestampsDocument {

        public long firstJoin;
        public long lastJoin;
        public long lastQuit;
        public long playTimeMillis;
        public long lastActivityAt;
        public @Nullable Long activeSessionStartedAt;

    }

    public static final class UserStateDocument {

        public boolean afk;
        public boolean god;
        public boolean flying;
        public boolean vanished;
        public @Nullable String nickname;
        public @Nullable Long personalTime;
        public @Nullable String personalWeather;
        public Map<String, List<String>> powerToolCommands = new LinkedHashMap<>();
        public Set<String> unlimitedItems = new LinkedHashSet<>();

    }

    public static final class UserPreferencesDocument {

        public boolean privateMessages = true;
        public boolean payments = true;
        public boolean teleportRequests = true;
        public boolean teleportAutoAccept;
        public boolean confirmLargePayments = true;
        public boolean confirmInventoryClears = true;
        public boolean replyToLastRecipient;
        public boolean powerToolsEnabled = true;
        public boolean socialSpy;
        public @Nullable String incomingReplyTarget;
        public @Nullable String outgoingReplyTarget;

    }

    public static final class UserRelationsDocument {

        public List<String> ignored = new ArrayList<>();

    }

}
