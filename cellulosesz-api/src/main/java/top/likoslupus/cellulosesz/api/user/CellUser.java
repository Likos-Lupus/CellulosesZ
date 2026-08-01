package top.likoslupus.cellulosesz.api.user;

import org.jspecify.annotations.Nullable;

import java.util.Map;
import java.util.UUID;

import static java.util.Objects.requireNonNull;

public record CellUser(
        UUID uuid,
        @Nullable String lastKnownName,
        UserTimestamps timestamps,
        UserState state,
        UserPreferences preferences,
        UserRelations relations,
        Map<String, Long> cooldowns
) {

    public CellUser {
        requireNonNull(uuid, "uuid");
        requireNonNull(timestamps, "timestamps");
        requireNonNull(state, "state");
        requireNonNull(preferences, "preferences");
        requireNonNull(relations, "relations");
        cooldowns = Map.copyOf(requireNonNull(cooldowns, "cooldowns"));
    }

    public static CellUser create(UUID uuid) {
        return new CellUser(
                uuid,
                null,
                UserTimestamps.defaults(),
                UserState.defaults(),
                UserPreferences.defaults(),
                UserRelations.defaults(),
                Map.of()
        );
    }

    public CellUser withLastKnownName(@Nullable String value) {
        return new CellUser(
                uuid,
                value,
                timestamps,
                state,
                preferences,
                relations,
                cooldowns
        );
    }

    public CellUser withTimestamps(UserTimestamps value) {
        return new CellUser(
                uuid,
                lastKnownName,
                value,
                state,
                preferences,
                relations,
                cooldowns
        );
    }

    public CellUser withState(UserState value) {
        return new CellUser(
                uuid,
                lastKnownName,
                timestamps,
                value,
                preferences,
                relations,
                cooldowns
        );
    }

    public CellUser withPreferences(UserPreferences value) {
        return new CellUser(
                uuid,
                lastKnownName,
                timestamps,
                state,
                value,
                relations,
                cooldowns
        );
    }

    public CellUser withRelations(UserRelations value) {
        return new CellUser(
                uuid,
                lastKnownName,
                timestamps,
                state,
                preferences,
                value,
                cooldowns
        );
    }

    public CellUser withCooldowns(Map<String, Long> value) {
        return new CellUser(
                uuid,
                lastKnownName,
                timestamps,
                state,
                preferences,
                relations,
                value
        );
    }

}
