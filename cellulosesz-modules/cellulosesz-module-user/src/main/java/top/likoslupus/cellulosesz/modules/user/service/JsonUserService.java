package top.likoslupus.cellulosesz.modules.user.service;

import top.likoslupus.cellulosesz.api.logging.CellulosesZLogger;
import top.likoslupus.cellulosesz.api.storage.StorageService;
import top.likoslupus.cellulosesz.api.user.CellUser;
import top.likoslupus.cellulosesz.api.user.NameCacheService;
import top.likoslupus.cellulosesz.api.user.UserService;

import java.nio.file.Path;
import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Function;

public final class JsonUserService implements UserService {

    private final StorageService storage;
    private final NameCacheService nameCache;
    private final Path usersDirectory;
    private final CellulosesZLogger logger;
    private final ConcurrentHashMap<UUID, CellUser> users = new ConcurrentHashMap<>();
    private final Set<UUID> dirty = ConcurrentHashMap.newKeySet();
    private final ConcurrentHashMap<UUID, CompletableFuture<Void>> updateTails = new ConcurrentHashMap<>();

    public JsonUserService(
            StorageService storage,
            NameCacheService nameCache,
            Path usersDirectory,
            CellulosesZLogger logger
    ) {
        this.storage = storage;
        this.nameCache = nameCache;
        this.usersDirectory = usersDirectory;
        this.logger = logger;
    }

    public void markQuit(Object player) {
        var uuid = PlayerIdentity.uuid(player);
        if (uuid.isEmpty()) return;

        var user = users.get(uuid.get());
        if (user == null) return;

        var now = System.currentTimeMillis();
        user.timestamps.lastQuit = now;
        if (user.timestamps.activeSessionStartedAt != null) {
            user.timestamps.playTimeMillis = Math.addExact(
                    user.timestamps.playTimeMillis,
                    Math.max(0L, now - user.timestamps.activeSessionStartedAt)
            );
            user.timestamps.activeSessionStartedAt = null;
        }
        markDirty(uuid.get());
    }

    @Override
    public CompletableFuture<CellUser> load(UUID uuid) {
        var cached = users.get(uuid);
        if (cached != null) return CompletableFuture.completedFuture(cached);

        return storage.load(userPath(uuid), CellUser.class, () -> create(uuid))
                .thenApply(user -> {
                    validate(user, uuid, true);
                    users.put(uuid, user);
                    if (user.lastKnownName != null) {
                        nameCache.remember(uuid, user.lastKnownName);
                    }
                    return user;
                });
    }

    @Override
    public CompletableFuture<CellUser> loadFromPlayer(Object player) {
        var resolvedUuid = PlayerIdentity.uuid(player);
        if (resolvedUuid.isEmpty()) {
            return CompletableFuture.failedFuture(new IllegalArgumentException("Unable to resolve player UUID from " + player));
        }

        var uuid = resolvedUuid.get();
        var name = PlayerIdentity.name(player);
        return load(uuid).thenApply(user -> {
            var now = System.currentTimeMillis();
            if (user.timestamps.firstJoin <= 0L) {
                user.timestamps.firstJoin = now;
            }

            user.timestamps.lastJoin = now;
            user.timestamps.lastActivityAt = now;
            user.timestamps.activeSessionStartedAt = now;
            name.ifPresent(value -> {
                user.lastKnownName = value;
                nameCache.remember(uuid, value);
            });

            markDirty(uuid);
            return user;
        });
    }

    @Override
    public Optional<CellUser> cached(UUID uuid) {
        return Optional.ofNullable(users.get(uuid));
    }

    @Override
    public Collection<CellUser> cachedUsers() {
        return List.copyOf(users.values());
    }

    @Override
    public Optional<UUID> findUuidByName(String name) {
        return nameCache.findUuid(name);
    }


    @Override
    public Collection<UUID> knownUuids() {
        var known = new LinkedHashSet<>(nameCache.entries().keySet());
        known.addAll(users.keySet());
        return Set.copyOf(known);
    }

    @Override
    public <T> CompletableFuture<T> update(UUID uuid, Function<CellUser, T> mutation) {
        Objects.requireNonNull(uuid, "uuid");
        Objects.requireNonNull(mutation, "mutation");
        var result = new CompletableFuture<T>();
        updateTails.compute(uuid, (ignored, previous) -> {
            var tail = previous == null ? CompletableFuture.completedFuture(null) : previous;
            var next = tail.handle((unused, failure) -> null)
                    .thenCompose(unused -> load(uuid))
                    .thenCompose(current -> {
                        CellUser replacement;
                        synchronized (current) {
                            replacement = copy(current);
                        }
                        final T value;
                        try {
                            value = mutation.apply(replacement);
                            validate(replacement, uuid, false);
                        } catch (RuntimeException exception) {
                            return CompletableFuture.<Void>failedFuture(exception);
                        }
                        return storage.save(userPath(uuid), replacement)
                                .thenRun(() -> {
                                    users.put(uuid, replacement);
                                    dirty.remove(uuid);
                                    result.complete(value);
                                });
                    });
            next.whenComplete((unused, failure) -> {
                updateTails.remove(uuid, next);
                if (failure != null) result.completeExceptionally(failure);
            });
            return next;
        });
        return result;
    }

    @Override
    public void markDirty(UUID uuid) {
        dirty.add(uuid);
    }


    @Override
    public CompletableFuture<Void> save(UUID uuid) {
        var user = users.get(uuid);
        if (user == null) {
            dirty.remove(uuid);
            return CompletableFuture.completedFuture(null);
        }

        return storage.save(userPath(uuid), user)
                .thenRun(() -> dirty.remove(uuid));
    }

    @Override
    public CompletableFuture<Void> saveAll() {
        var userFutures = dirty.stream()
                .map(this::save)
                .toArray(CompletableFuture[]::new);

        return CompletableFuture.allOf(userFutures)
                .thenCompose(_ -> nameCache.save())
                .whenComplete((_, exception) -> {
                    if (exception != null) {
                        logger.error("Failed to save user data", exception);
                    }
                });
    }

    private CellUser create(UUID uuid) {
        var user = new CellUser(uuid);
        var now = System.currentTimeMillis();
        user.timestamps.firstJoin = now;
        return user;
    }

    private void validate(CellUser user, UUID expectedUuid, boolean resetInterruptedSession) {
        Objects.requireNonNull(user, "user");
        if (!expectedUuid.equals(user.uuid)) {
            throw new IllegalArgumentException("User document UUID does not match its file name");
        }
        Objects.requireNonNull(user.timestamps, "timestamps");
        Objects.requireNonNull(user.state, "state");
        Objects.requireNonNull(user.preferences, "preferences");
        Objects.requireNonNull(user.relations, "relations");
        Objects.requireNonNull(user.cooldowns, "cooldowns");
        if (user.timestamps.playTimeMillis < 0L) {
            throw new IllegalArgumentException("playTimeMillis must not be negative");
        }
        // Only a document read can contain an interrupted session. Normal in-memory updates must preserve it.
        if (resetInterruptedSession) user.timestamps.activeSessionStartedAt = null;
    }

    private CellUser copy(CellUser source) {
        var copy = new CellUser(source.uuid);
        copy.lastKnownName = source.lastKnownName;

        copy.timestamps.firstJoin = source.timestamps.firstJoin;
        copy.timestamps.lastJoin = source.timestamps.lastJoin;
        copy.timestamps.lastQuit = source.timestamps.lastQuit;
        copy.timestamps.playTimeMillis = source.timestamps.playTimeMillis;
        copy.timestamps.lastActivityAt = source.timestamps.lastActivityAt;
        copy.timestamps.activeSessionStartedAt = source.timestamps.activeSessionStartedAt;

        copy.state.afk = source.state.afk;
        copy.state.god = source.state.god;
        copy.state.flying = source.state.flying;
        copy.state.vanished = source.state.vanished;
        copy.state.nickname = source.state.nickname;
        copy.state.personalTime = source.state.personalTime;
        copy.state.personalWeather = source.state.personalWeather;
        source.state.powerToolCommands.forEach((item, commands) ->
                copy.state.powerToolCommands.put(item, new ArrayList<>(commands))
        );
        copy.state.unlimitedItems.addAll(source.state.unlimitedItems);

        copy.preferences.privateMessages = source.preferences.privateMessages;
        copy.preferences.payments = source.preferences.payments;
        copy.preferences.teleportRequests = source.preferences.teleportRequests;
        copy.preferences.teleportAutoAccept = source.preferences.teleportAutoAccept;
        copy.preferences.confirmLargePayments = source.preferences.confirmLargePayments;
        copy.preferences.replyToLastRecipient = source.preferences.replyToLastRecipient;
        copy.preferences.powerToolsEnabled = source.preferences.powerToolsEnabled;
        copy.preferences.socialSpy = source.preferences.socialSpy;
        copy.preferences.incomingReplyTarget = source.preferences.incomingReplyTarget;
        copy.preferences.outgoingReplyTarget = source.preferences.outgoingReplyTarget;

        copy.relations.ignored.addAll(source.relations.ignored);
        copy.cooldowns.putAll(source.cooldowns);
        return copy;
    }

    private Path userPath(UUID uuid) {
        return usersDirectory.resolve(uuid + ".json");
    }

}
