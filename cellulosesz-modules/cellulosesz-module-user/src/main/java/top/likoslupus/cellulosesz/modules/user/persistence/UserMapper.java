package top.likoslupus.cellulosesz.modules.user.persistence;

import top.likoslupus.cellulosesz.api.user.*;

import java.util.*;
import java.util.stream.Collectors;
import org.jspecify.annotations.Nullable;

import static java.util.Objects.requireNonNull;

/** Explicit mapping between the user JSON schema and immutable domain values. */
public final class UserMapper {

    private UserMapper() {
        throw new AssertionError("No instances");
    }

    public static CellUser toDomain(UserDocument document) {
        requireNonNull(document, "document");
        try {
            var timestamps = requireNonNull(document.timestamps, "timestamps");
            var state = requireNonNull(document.state, "state");
            var preferences = requireNonNull(document.preferences, "preferences");
            var relations = requireNonNull(document.relations, "relations");

            var powerTools = new LinkedHashMap<String, List<String>>();
            requireNonNull(state.powerToolCommands, "state.powerToolCommands")
                    .forEach((item, commands) -> powerTools.put(
                            requireNonNull(item, "power tool item"),
                            List.copyOf(requireNonNull(commands, "power tool commands"))
                    ));

            var ignored = requireNonNull(relations.ignored, "relations.ignored").stream()
                    .map(value -> parseUuid(value, "relations.ignored"))
                    .collect(Collectors.toCollection(LinkedHashSet::new));

            return new CellUser(
                    parseUuid(document.uuid, "uuid"),
                    document.lastKnownName,
                    new UserTimestamps(
                            timestamps.firstJoin,
                            timestamps.lastJoin,
                            timestamps.lastQuit,
                            timestamps.playTimeMillis,
                            timestamps.lastActivityAt,
                            timestamps.activeSessionStartedAt
                    ),
                    new UserState(
                            state.afk,
                            state.god,
                            state.flying,
                            state.vanished,
                            state.nickname,
                            state.personalTime,
                            state.personalWeather,
                            powerTools,
                            requireNonNull(state.unlimitedItems, "state.unlimitedItems")
                    ),
                    new UserPreferences(
                            preferences.privateMessages,
                            preferences.payments,
                            preferences.teleportRequests,
                            preferences.teleportAutoAccept,
                            preferences.confirmLargePayments,
                            preferences.confirmInventoryClears,
                            preferences.replyToLastRecipient,
                            preferences.powerToolsEnabled,
                            preferences.socialSpy,
                            parseOptionalUuid(
                                    preferences.incomingReplyTarget,
                                    "preferences.incomingReplyTarget"
                            ),
                            parseOptionalUuid(
                                    preferences.outgoingReplyTarget,
                                    "preferences.outgoingReplyTarget"
                            )
                    ),
                    new UserRelations(ignored),
                    requireNonNull(document.cooldowns, "cooldowns")
            );
        } catch (RuntimeException failure) {
            throw new IllegalArgumentException("Invalid persisted user document", failure);
        }
    }

    private static UUID parseUuid(@Nullable String value, String field) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException(field + " must not be blank");
        }

        try {
            return UUID.fromString(value);
        } catch (IllegalArgumentException failure) {
            throw new IllegalArgumentException("Invalid UUID in " + field + ": " + value, failure);
        }
    }

    private static @Nullable UUID parseOptionalUuid(@Nullable String value, String field) {
        return value == null
                ? null
                : parseUuid(value, field);
    }

    public static UserDocument fromDomain(CellUser user) {
        requireNonNull(user, "user");
        var document = new UserDocument();
        document.uuid = user.uuid().toString();
        document.lastKnownName = user.lastKnownName();

        var timestamps = new UserDocument.UserTimestampsDocument();
        timestamps.firstJoin = user.timestamps().firstJoin();
        timestamps.lastJoin = user.timestamps().lastJoin();
        timestamps.lastQuit = user.timestamps().lastQuit();
        timestamps.playTimeMillis = user.timestamps().playTimeMillis();
        timestamps.lastActivityAt = user.timestamps().lastActivityAt();
        timestamps.activeSessionStartedAt = user.timestamps().activeSessionStartedAt();
        document.timestamps = timestamps;

        var state = new UserDocument.UserStateDocument();
        state.afk = user.state().afk();
        state.god = user.state().god();
        state.flying = user.state().flying();
        state.vanished = user.state().vanished();
        state.nickname = user.state().nickname();
        state.personalTime = user.state().personalTime();
        state.personalWeather = user.state().personalWeather();
        user.state().powerToolCommands().forEach((item, commands) ->
                state.powerToolCommands.put(item, new ArrayList<>(commands))
        );
        state.unlimitedItems.addAll(user.state().unlimitedItems());
        document.state = state;

        var preferences = new UserDocument.UserPreferencesDocument();
        preferences.privateMessages = user.preferences().privateMessages();
        preferences.payments = user.preferences().payments();
        preferences.teleportRequests = user.preferences().teleportRequests();
        preferences.teleportAutoAccept = user.preferences().teleportAutoAccept();
        preferences.confirmLargePayments = user.preferences().confirmLargePayments();
        preferences.confirmInventoryClears = user.preferences().confirmInventoryClears();
        preferences.replyToLastRecipient = user.preferences().replyToLastRecipient();
        preferences.powerToolsEnabled = user.preferences().powerToolsEnabled();
        preferences.socialSpy = user.preferences().socialSpy();
        preferences.incomingReplyTarget = toText(user.preferences().incomingReplyTarget());
        preferences.outgoingReplyTarget = toText(user.preferences().outgoingReplyTarget());
        document.preferences = preferences;

        var relations = new UserDocument.UserRelationsDocument();
        user.relations().ignored().stream().map(UUID::toString).forEach(relations.ignored::add);
        document.relations = relations;
        document.cooldowns.putAll(user.cooldowns());
        return document;
    }

    private static @Nullable String toText(@Nullable UUID value) {
        return value == null
                ? null
                : value.toString();
    }

}
